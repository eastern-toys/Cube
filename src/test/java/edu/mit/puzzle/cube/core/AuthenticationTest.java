package edu.mit.puzzle.cube.core;
import org.apache.shiro.authc.AuthenticationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;

public class AuthenticationTest extends RestletTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testNoCredentials() {
        currentUserCredentials = null;
        exception.expect(AuthenticationException.class);
        get("/user/testerteam");
    }

    @Test
    public void testUnknownUser() {
        currentUserCredentials = new ChallengeResponse(
                ChallengeScheme.HTTP_BASIC,
                "unknown",
                "password"
        );
        exception.expect(AuthenticationException.class);
        get("/user/testerteam");
    }

    @Test
    public void testWrongPassword() {
        currentUserCredentials = new ChallengeResponse(
                ChallengeScheme.HTTP_BASIC,
                TESTERTEAM_CREDENTIALS.getIdentifier(),
                "wrongpassword"
        );
        exception.expect(AuthenticationException.class);
        get("/user/testerteam");
    }
}
