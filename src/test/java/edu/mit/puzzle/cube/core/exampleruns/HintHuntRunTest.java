package edu.mit.puzzle.cube.core.exampleruns;

import com.fasterxml.jackson.databind.JsonNode;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.RestletTest;
import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;
import edu.mit.puzzle.cube.core.model.HintRequest;
import edu.mit.puzzle.cube.core.model.HintRequestStatus;
import edu.mit.puzzle.cube.huntimpl.hintexample.HintExampleHuntDefinition;

import org.apache.shiro.realm.Realm;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class HintHuntRunTest extends RestletTest {
    protected static final ChallengeResponse TESTERTEAM_CREDENTIALS =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "testerteam", "testerteampassword");

    @Override
    protected HuntDefinition createHuntDefinition() {
        return new HintExampleHuntDefinition();
    }

    @Override
    protected Realm createAuthenticationRealm() {
        CubeJdbcRealm realm = new CubeJdbcRealm();
        realm.setDataSource(serviceEnvironment.getConnectionFactory().getDataSource());
        return realm;
    }

    private int getTokens() {
        JsonNode json = get("/teams/testerteam");
        return json.get("teamProperties").get("HintTokensProperty").get("tokens").asInt();
    }

    @Test
    public void testHintRun() throws IOException {
        postHuntStart();

        // Every team starts with one hint token.
        assertThat(getTokens()).isEqualTo(1);

        JsonNode json = getVisibility("testerteam", "meta");
        assertThat(json.get("status").asText()).isEqualTo("INVISIBLE");

        currentUserCredentials = TESTERTEAM_CREDENTIALS;
        postNewSubmission("testerteam", "puzzle1", "ANSWER1");
        currentUserCredentials = ADMIN_CREDENTIALS;
        postUpdateSubmission(1, "CORRECT");

        // We gain a hint token on every correct submission.
        assertThat(getTokens()).isEqualTo(2);

        json = getVisibility("testerteam", "meta");
        assertThat(json.get("status").asText()).isEqualTo("UNLOCKED");

        currentUserCredentials = TESTERTEAM_CREDENTIALS;

        // Attempt to request a hint for the meta. It's not allowed.
        json = post(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId("testerteam")
                        .setPuzzleId("meta")
                        .setRequest("help")
                        .build()
        );
        assertThat(json.get("created").asBoolean()).isFalse();
        assertThat(getTokens()).isEqualTo(2);

        // Request a hint for puzzle 1. Should be rejected because we've already solved the puzzle.
        postExpectFailure(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId("testerteam")
                        .setPuzzleId("puzzle1")
                        .setRequest("help")
                        .build()
        );
        assertThat(getTokens()).isEqualTo(2);

        // Request a hint for puzzle 2. This will succeed, and a token will be deducted.
        json = post(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId("testerteam")
                        .setPuzzleId("puzzle2")
                        .setRequest("help")
                        .build()
        );
        assertThat(json.get("created").asBoolean()).isTrue();
        assertThat(getTokens()).isEqualTo(1);

        // Reject the hint request. A token should be credited back to the team.
        currentUserCredentials = ADMIN_CREDENTIALS;
        post(
                "/hintrequests/1",
                HintRequest.builder()
                        .setHintRequestId(1)
                        .setCallerUsername("adminuser")
                        .setStatus(HintRequestStatus.REJECTED)
                        .build()
        );
        assertThat(getTokens()).isEqualTo(2);

        // Request another hint for puzzle 2. This will succeed, and a token will be deducted.
        currentUserCredentials = TESTERTEAM_CREDENTIALS;
        json = post(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId("testerteam")
                        .setPuzzleId("puzzle2")
                        .setRequest("help")
                        .build()
        );
        assertThat(json.get("created").asBoolean()).isTrue();
        assertThat(getTokens()).isEqualTo(1);

        // Successfully complete the hint request.
        currentUserCredentials = ADMIN_CREDENTIALS;
        post(
                "/hintrequests/2",
                HintRequest.builder()
                        .setHintRequestId(1)
                        .setCallerUsername("adminuser")
                        .setResponse("response")
                        .setStatus(HintRequestStatus.ANSWERED)
                        .build()
        );
        assertThat(getTokens()).isEqualTo(1);

        // Request a hint for puzzle 3.
        currentUserCredentials = TESTERTEAM_CREDENTIALS;
        json = post(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId("testerteam")
                        .setPuzzleId("puzzle3")
                        .setRequest("help")
                        .build()
        );
        assertThat(json.get("created").asBoolean()).isTrue();
        assertThat(getTokens()).isEqualTo(0);

        // Request another hint for puzzle 2. Will fail due to lack of tokens.
        currentUserCredentials = TESTERTEAM_CREDENTIALS;
        json = post(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId("testerteam")
                        .setPuzzleId("puzzle2")
                        .setRequest("help")
                        .build()
        );
        assertThat(json.get("created").asBoolean()).isFalse();
        assertThat(getTokens()).isEqualTo(0);
    }
}
