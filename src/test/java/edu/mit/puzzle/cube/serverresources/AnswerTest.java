package edu.mit.puzzle.cube.serverresources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.RestletTest;
import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.model.Answer;
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

public class AnswerTest extends RestletTest {
    private static final String PUZZLE_ONE = "puzzle1";
    private static final String PUZZLE_TWO = "puzzle2";

    private static final String ANSWER_ONE = "ANSWER1";
    private static final String ANSWER_TWO = "ANSWER2";

    private static final ChallengeResponse WRITING_USER =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "writinguser", "writinguserpassword");
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
            public List<Answer> getPuzzleList() {
                return ImmutableList.of(
                        Answer.create(PUZZLE_ONE, ANSWER_ONE),
                        Answer.create(PUZZLE_TWO, ANSWER_TWO)
                );
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
        addUser(WRITING_USER, ImmutableList.of("writingteam"));
        addTeam(TEAM);
        postHuntStart();
        postVisibility(TEAM.getIdentifier(), PUZZLE_ONE, "UNLOCKED");
    }

    @Test
    public void testGetAnswer() {
        setCurrentUserCredentials(WRITING_USER);

        getExpectFailure("/answer/badpuzzleid");

        JsonNode answerJson = get("/answer/" + PUZZLE_ONE);
        assertThat(answerJson.get("puzzleId").asText()).isEqualTo(PUZZLE_ONE);
        assertThat(answerJson.get("canonicalAnswers").size()).isEqualTo(1);
        assertThat(answerJson.get("canonicalAnswers").get(0).asText()).isEqualTo(ANSWER_ONE);
        assertThat(answerJson.get("acceptableAnswers").size()).isEqualTo(1);
        assertThat(answerJson.get("acceptableAnswers").get(0).asText()).isEqualTo(ANSWER_ONE);

        answerJson = get("/answer/" + PUZZLE_TWO);
        assertThat(answerJson.get("puzzleId").asText()).isEqualTo(PUZZLE_TWO);
        assertThat(answerJson.get("canonicalAnswers").size()).isEqualTo(1);
        assertThat(answerJson.get("canonicalAnswers").get(0).asText()).isEqualTo(ANSWER_TWO);
        assertThat(answerJson.get("acceptableAnswers").size()).isEqualTo(1);
        assertThat(answerJson.get("acceptableAnswers").get(0).asText()).isEqualTo(ANSWER_TWO);

        setCurrentUserCredentials(TEAM);
        getExpectFailure("/answer/" + PUZZLE_ONE);

        setCurrentUserCredentials(ADMIN_CREDENTIALS);
        postVisibility("team", PUZZLE_ONE, "SOLVED");

        setCurrentUserCredentials(TEAM);

        answerJson = get("/answer/" + PUZZLE_ONE);
        assertThat(answerJson.get("puzzleId").asText()).isEqualTo(PUZZLE_ONE);
        assertThat(answerJson.get("canonicalAnswers").size()).isEqualTo(1);
        assertThat(answerJson.get("canonicalAnswers").get(0).asText()).isEqualTo(ANSWER_ONE);
        assertThat(answerJson.get("acceptableAnswers").size()).isEqualTo(1);
        assertThat(answerJson.get("acceptableAnswers").get(0).asText()).isEqualTo(ANSWER_ONE);

        getExpectFailure("/answer/" + PUZZLE_TWO);
    }
}
