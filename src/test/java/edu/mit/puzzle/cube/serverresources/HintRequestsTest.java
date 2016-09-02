package edu.mit.puzzle.cube.serverresources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.RestletTest;
import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.model.HintRequest;
import edu.mit.puzzle.cube.core.model.HintRequestStatus;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.apache.shiro.realm.Realm;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;

import java.sql.SQLException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class HintRequestsTest extends RestletTest {
    private static final String PUZZLE_ID = "puzzle1";

    private static final ChallengeResponse USER_ONE =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "userone", "useronepassword");
    private static final ChallengeResponse USER_TWO =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "usertwo", "usertwopassword");
    private static final ChallengeResponse TEAM =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "team", "teampassword");

    @Override
    protected Realm createAuthenticationRealm() {
        CubeJdbcRealm realm = new CubeJdbcRealm();
        realm.setDataSource(serviceEnvironment.getConnectionFactory().getDataSource());
        return realm;
    }

    protected HuntDefinition createHuntDefinition() {
        return new HuntDefinition() {
            @Override
            public VisibilityStatusSet getVisibilityStatusSet() {
                return new StandardVisibilityStatusSet();
            }

            @Override
            public List<Puzzle> getPuzzles() {
                return ImmutableList.of(Puzzle.create(PUZZLE_ID, "ANSWER"));
            }

            @Override
            public void addToEventProcessor(
                    CompositeEventProcessor eventProcessor,
                    HuntStatusStore huntStatusStore
            ) {
            }
        };
    }

    public void setUp() throws SQLException {
        super.setUp();
        addUser(USER_ONE, ImmutableList.of("writingteam"));
        addUser(USER_TWO, ImmutableList.of("writingteam"));
        addTeam(TEAM);
        postHuntStart();
    }

    @Test
    public void testRequestHintForInvisiblePuzzle() {
        setCurrentUserCredentials(TEAM);
        Status status = postExpectFailure(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId(TEAM.getIdentifier())
                        .setPuzzleId(PUZZLE_ID)
                        .setRequest("help")
                        .build()
        );
        assertThat(status.getCode()).isEqualTo(400);
    }

    @Test
    public void testRequestAndResolveHint() {
        postVisibility(TEAM.getIdentifier(), PUZZLE_ID, "UNLOCKED");

        setCurrentUserCredentials(TEAM);
        post(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId(TEAM.getIdentifier())
                        .setPuzzleId(PUZZLE_ID)
                        .setRequest("help")
                        .build()
        );

        JsonNode teamGetResult = get(
                String.format("/hintrequests?teamId=%s&puzzleId=%s", TEAM.getIdentifier(), PUZZLE_ID)
        );
        assertThat(teamGetResult.get("hintRequests").size()).isEqualTo(1);
        JsonNode hintRequest = teamGetResult.get("hintRequests").get(0);
        assertThat(hintRequest.get("teamId").asText()).isEqualTo(TEAM.getIdentifier());
        assertThat(hintRequest.get("puzzleId").asText()).isEqualTo(PUZZLE_ID);
        assertThat(hintRequest.get("status").asText()).isEqualTo(HintRequestStatus.REQUESTED.name());
        assertThat(hintRequest.get("request").asText()).isEqualTo("help");

        setCurrentUserCredentials(USER_ONE);
        post(
                String.format("/hintrequests/%d", hintRequest.get("hintRequestId").asInt()),
                HintRequest.builder()
                        .setHintRequestId(hintRequest.get("hintRequestId").asInt())
                        .setStatus(HintRequestStatus.ASSIGNED)
                        .build()
        );

        JsonNode userGetResult = get(
                String.format("/hintrequests", TEAM.getIdentifier(), PUZZLE_ID)
        );
        assertThat(userGetResult.get("hintRequests").size()).isEqualTo(1);
        hintRequest = userGetResult.get("hintRequests").get(0);
        assertThat(hintRequest.get("teamId").asText()).isEqualTo(TEAM.getIdentifier());
        assertThat(hintRequest.get("puzzleId").asText()).isEqualTo(PUZZLE_ID);
        assertThat(hintRequest.get("status").asText()).isEqualTo(HintRequestStatus.ASSIGNED.name());
        assertThat(hintRequest.get("callerUsername").asText()).isEqualTo(USER_ONE.getIdentifier());
        assertThat(hintRequest.get("request").asText()).isEqualTo("help");

        post(
                String.format("/hintrequests/%d", hintRequest.get("hintRequestId").asInt()),
                HintRequest.builder()
                        .setHintRequestId(hintRequest.get("hintRequestId").asInt())
                        .setResponse("response")
                        .setStatus(HintRequestStatus.ANSWERED)
                        .build()
        );

        userGetResult = get(
                String.format("/hintrequests", TEAM.getIdentifier(), PUZZLE_ID)
        );
        assertThat(userGetResult.get("hintRequests").size()).isEqualTo(0);

        setCurrentUserCredentials(TEAM);
        teamGetResult = get(
                String.format("/hintrequests?teamId=%s&puzzleId=%s", TEAM.getIdentifier(), PUZZLE_ID)
        );
        assertThat(teamGetResult.get("hintRequests").size()).isEqualTo(1);
        hintRequest = teamGetResult.get("hintRequests").get(0);
        assertThat(hintRequest.get("teamId").asText()).isEqualTo(TEAM.getIdentifier());
        assertThat(hintRequest.get("puzzleId").asText()).isEqualTo(PUZZLE_ID);
        assertThat(hintRequest.get("status").asText()).isEqualTo(HintRequestStatus.ANSWERED.name());
        assertThat(hintRequest.get("request").asText()).isEqualTo("help");
        assertThat(hintRequest.get("response").asText()).isEqualTo("response");
    }
}
