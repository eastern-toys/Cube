package edu.mit.puzzle.cube.serverresources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.RestletTest;
import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.apache.shiro.realm.Realm;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;

import java.sql.SQLException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class SubmissionsTest extends RestletTest {
    private static final String PUZZLE_ID = "puzzle1";

    private static final ChallengeResponse TEAM =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "team", "teampassword");
    private static final ChallengeResponse TEAM2 =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "team2", "team2password");

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
            public List<String> getPuzzleList() {
                return ImmutableList.<String>of(PUZZLE_ID);
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
        addTeam(TEAM);
        addTeam(TEAM2);
        postHuntStart();
        postVisibility(TEAM.getIdentifier(), PUZZLE_ID, "UNLOCKED");
        postVisibility(TEAM2.getIdentifier(), PUZZLE_ID, "UNLOCKED");
    }

    @Test
    public void testTeamIdAndStatus() {
        setCurrentUserCredentials(TEAM);
        postNewSubmission(TEAM.getIdentifier(), PUZZLE_ID, "teamanswer");

        setCurrentUserCredentials(TEAM2);
        postNewSubmission(TEAM2.getIdentifier(), PUZZLE_ID, "team2answer");

        JsonNode response = get("/submissions?teamId=team2&status=SUBMITTED");
        ArrayNode submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(1);

        response = get("/submissions?teamId=team2&status=ASSIGNED");
        submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(0);

        setCurrentUserCredentials(ADMIN_CREDENTIALS);

        response = get("/submissions?status=SUBMITTED");
        submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(2);

        response = get("/submissions?status=ASSIGNED");
        submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(0);

        response = get("/submissions?status=SUBMITTED,ASSIGNED");
        submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(2);
    }

    @Test
    public void testPagination() {
        setCurrentUserCredentials(TEAM);
        for (int i = 0; i < 20; i++) {
            postNewSubmission(TEAM.getIdentifier(), PUZZLE_ID, String.format("answer%d", i));
        }

        setCurrentUserCredentials(TEAM2);
        for (int i = 0; i < 20; i++) {
            postNewSubmission(TEAM2.getIdentifier(), PUZZLE_ID, String.format("answer%d", i));
        }

        setCurrentUserCredentials(ADMIN_CREDENTIALS);

        JsonNode response = get("/submissions?pageSize=10");
        ArrayNode submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(10);
        assertThat(submissions.get(0).get("teamId").asText()).isEqualTo("team");
        assertThat(submissions.get(0).get("submission").asText()).isEqualTo("answer0");
        assertThat(submissions.get(9).get("teamId").asText()).isEqualTo("team");
        assertThat(submissions.get(9).get("submission").asText()).isEqualTo("answer9");

        response = get("/submissions?teamId=team&startSubmissionId=10&pageSize=10");
        submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(10);
        assertThat(submissions.get(0).get("submission").asText()).isEqualTo("answer10");
        assertThat(submissions.get(9).get("submission").asText()).isEqualTo("answer19");

        response = get("/submissions?teamId=team&startSubmissionId=20&pageSize=10");
        submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(0);

        response = get("/submissions?teamId=team2&pageSize=10");
        submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(10);
        assertThat(submissions.get(0).get("teamId").asText()).isEqualTo("team2");
        assertThat(submissions.get(0).get("submission").asText()).isEqualTo("answer0");
        assertThat(submissions.get(9).get("teamId").asText()).isEqualTo("team2");
        assertThat(submissions.get(9).get("submission").asText()).isEqualTo("answer9");

        response = get("/submissions?teamId=team2&startSubmissionId=30&pageSize=10");
        submissions = (ArrayNode) response.get("submissions");
        assertThat(submissions.size()).isEqualTo(10);
        assertThat(submissions.get(0).get("teamId").asText()).isEqualTo("team2");
        assertThat(submissions.get(0).get("submission").asText()).isEqualTo("answer10");
        assertThat(submissions.get(9).get("teamId").asText()).isEqualTo("team2");
        assertThat(submissions.get(9).get("submission").asText()).isEqualTo("answer19");
    }
}
