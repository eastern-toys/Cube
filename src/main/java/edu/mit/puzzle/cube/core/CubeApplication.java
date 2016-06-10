package edu.mit.puzzle.cube.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.DevelopmentEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.PeriodicTimerEvent;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.UserStore;
import edu.mit.puzzle.cube.core.serverresources.*;
import edu.mit.puzzle.cube.huntimpl.linearexample.LinearExampleHuntDefinition;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.jdbc.JdbcRealm;
import org.apache.shiro.subject.Subject;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;
import org.restlet.service.CorsService;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class CubeApplication extends Application {

    private final SubmissionStore submissionStore;
    private final HuntStatusStore huntStatusStore;
    private final UserStore userStore;
    private final CompositeEventProcessor eventProcessor;

    private final Service timingEventService;

    public CubeApplication() throws SQLException {
        CorsService corsService = new CorsService();
        corsService.setAllowedOrigins(ImmutableSet.of("*", "http://localhost:8081"));
        corsService.setAllowedCredentials(true);
        corsService.setAllowingAllRequestedHeaders(true);
        getServices().add(corsService);

        setStatusService(new CubeStatusService(corsService));

        HuntDefinition huntDefinition = new LinearExampleHuntDefinition();
        ServiceEnvironment serviceEnvironment = new DevelopmentEnvironment(huntDefinition);

        ConnectionFactory connectionFactory = serviceEnvironment.getConnectionFactory();

        setupAuthentication(connectionFactory);

        eventProcessor = new CompositeEventProcessor();
        submissionStore = new SubmissionStore(
                connectionFactory,
                eventProcessor
        );
        huntStatusStore = new HuntStatusStore(
                connectionFactory,
                huntDefinition.getVisibilityStatusSet(),
                eventProcessor
        );
        userStore = new UserStore(
                connectionFactory
        );

        huntDefinition.addToEventProcessor(
                eventProcessor,
                huntStatusStore
        );

        timingEventService = new AbstractScheduledService() {
            @Override
            protected void runOneIteration() throws Exception {
                eventProcessor.process(PeriodicTimerEvent.builder().build());
            }

            @Override
            protected Scheduler scheduler() {
                return Scheduler.newFixedRateSchedule(0, 10, TimeUnit.SECONDS);
            }
        };
        timingEventService.startAsync();
    }

    private void setupAuthentication(ConnectionFactory connectionFactory) {
        JdbcRealm realm = new JdbcRealm();
        realm.setCredentialsMatcher(new HashedCredentialsMatcher("SHA-512"));
        realm.setDataSource(connectionFactory.getDataSource());
        realm.setPermissionsLookupEnabled(true);
        realm.setSaltStyle(JdbcRealm.SaltStyle.COLUMN);

        SecurityManager securityManager = new DefaultSecurityManager(realm);
        SecurityUtils.setSecurityManager(securityManager);
    }

    @Override
    public synchronized Restlet createInboundRoot() {
        // Create a router Restlet that routes each call to a new instance of HelloWorldResource.
        Router router = new Router(getContext());

        //Put dependencies into the router context so that the Resource handlers can access them
        router.getContext().getAttributes().put(AbstractCubeResource.SUBMISSION_STORE_KEY, submissionStore);
        router.getContext().getAttributes().put(AbstractCubeResource.HUNT_STATUS_STORE_KEY, huntStatusStore);
        router.getContext().getAttributes().put(AbstractCubeResource.USER_STORE_KEY, userStore);
        router.getContext().getAttributes().put(AbstractCubeResource.EVENT_PROCESSOR_KEY, eventProcessor);

        //Define routes
        router.attach("/submissions", SubmissionsResource.class);
        router.attach("/submissions/{id}", SubmissionResource.class);
        router.attach("/visibilities", VisibilitiesResource.class);
        router.attach("/visibilities/{teamId}/{puzzleId}", VisibilityResource.class);
        router.attach("/visibilitychanges", VisibilityChangesResource.class);
        router.attach("/events", EventsResource.class);
        router.attach("/teams", TeamsResource.class);
        router.attach("/teams/{id}", TeamResource.class);
        router.attach("/users/{id}", UserResource.class);
        router.attach("/authorized", AuthorizedResource.class);

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

        return authenticator;
    }

    public static void main (String[] args) throws Exception {
        // Create a new Component.
        Component component = new Component();

        // Add a new HTTP server listening on port 8182.
        component.getServers().add(Protocol.HTTP, 8182);

        // Attach this application.
        component.getDefaultHost().attach("", new CubeApplication());

        // Start the component.
        component.start();
    }

}
