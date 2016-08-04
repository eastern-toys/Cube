package edu.mit.puzzle.cube.core;

import edu.mit.puzzle.cube.core.serverresources.*;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;

public class CubeRestlet extends Filter {
    public CubeRestlet(Context context) {
        super(context);

        Router router = new Router(context);
        router.attach("/answer/{id}", AnswerResource.class);
        router.attach("/authorized", AuthorizedResource.class);
        router.attach("/events", EventsResource.class);
        router.attach("/submissions", SubmissionsResource.class);
        router.attach("/submissions/{id}", SubmissionResource.class);
        router.attach("/teams", TeamsResource.class);
        router.attach("/teams/{id}", TeamResource.class);
        router.attach("/users", UsersResource.class);
        router.attach("/users/{id}", UserResource.class);
        router.attach("/visibilities", VisibilitiesResource.class);
        router.attach("/visibilities/{teamId}/{puzzleId}", VisibilityResource.class);
        router.attach("/visibilitychanges", VisibilityChangesResource.class);

        // Create an authenticator for all routes.
        ChallengeAuthenticator authenticator = new ChallengeAuthenticator(
                null,
                ChallengeScheme.HTTP_BASIC,
                "Cube"
        );
        authenticator.setVerifier((Request request, Response response) -> {
            if (request.getMethod().equals(Method.OPTIONS)) {
                return Verifier.RESULT_VALID;
            }

            Subject subject = SecurityUtils.getSubject();

            // We don't want any sessionization for our stateless RESTful API.
            if (subject.isAuthenticated() && subject.getSession(false) != null) {
                subject.logout();
            }

            ChallengeResponse challengeResponse = request.getChallengeResponse();
            if (challengeResponse == null) {
                throw new AuthenticationException(
                        "Credentials are required, but none were provided.");
            }

            UsernamePasswordToken token = new UsernamePasswordToken(
                    challengeResponse.getIdentifier(),
                    challengeResponse.getSecret()
            );
            subject.login(token);

            return Verifier.RESULT_VALID;
        });
        authenticator.setNext(router);

        setNext(authenticator);
    }
}
