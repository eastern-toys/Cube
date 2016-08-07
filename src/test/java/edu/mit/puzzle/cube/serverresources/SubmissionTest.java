package edu.mit.puzzle.cube.serverresources;

import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.RestletTest;
import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.apache.shiro.realm.Realm;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;

import java.sql.SQLException;
import java.util.List;

public class SubmissionTest extends RestletTest {
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
        postVisibility(TEAM.getIdentifier(), PUZZLE_ID, "UNLOCKED");
    }

    @Test
    public void testMustUnassignBeforeReclaim() {
        setCurrentUserCredentials(TEAM);
        postNewSubmission(TEAM.getIdentifier(), PUZZLE_ID, "answer");

        setCurrentUserCredentials(USER_ONE);
        postUpdateSubmission(1, SubmissionStatus.ASSIGNED.name());

        setCurrentUserCredentials(USER_TWO);
        postExpectFailure("/submissions/1", "{\"status\":\"ASSIGNED\"}");

        postUpdateSubmission(1, SubmissionStatus.SUBMITTED.name());
        postUpdateSubmission(1, SubmissionStatus.ASSIGNED.name());

        setCurrentUserCredentials(USER_ONE);
        postExpectFailure("/submissions/1", "{\"status\":\"ASSIGNED\"}");
    }

    @Test
    public void testDoNotUpdateIfTerminal() {
        setCurrentUserCredentials(TEAM);
        postNewSubmission(TEAM.getIdentifier(), PUZZLE_ID, "answer");

        setCurrentUserCredentials(USER_ONE);
        postUpdateSubmission(1, SubmissionStatus.ASSIGNED.name());
        postUpdateSubmission(1, SubmissionStatus.CORRECT.name());
        postExpectFailure("/submissions/1", "{\"status\":\"ASSIGNED\"}");

        setCurrentUserCredentials(USER_TWO);
        postExpectFailure("/submissions/1", "{\"status\":\"ASSIGNED\"}");
    }
}
