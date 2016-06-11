package edu.mit.puzzle.cube.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;
import edu.mit.puzzle.cube.core.environments.DevelopmentEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.PeriodicTimerEvent;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.UserStore;
import edu.mit.puzzle.cube.core.serverresources.AbstractCubeResource;
import edu.mit.puzzle.cube.huntimpl.linearexample.LinearExampleHuntDefinition;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
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
        CubeJdbcRealm realm = new CubeJdbcRealm();
        realm.setDataSource(connectionFactory.getDataSource());

        SecurityManager securityManager = new DefaultSecurityManager(realm);
        SecurityUtils.setSecurityManager(securityManager);
    }

    @Override
    public synchronized Restlet createInboundRoot() {
        // Put dependencies into the context so that the Resource handlers can access them.
        getContext().getAttributes().put(AbstractCubeResource.SUBMISSION_STORE_KEY, submissionStore);
        getContext().getAttributes().put(AbstractCubeResource.HUNT_STATUS_STORE_KEY, huntStatusStore);
        getContext().getAttributes().put(AbstractCubeResource.USER_STORE_KEY, userStore);
        getContext().getAttributes().put(AbstractCubeResource.EVENT_PROCESSOR_KEY, eventProcessor);

        return new CubeRestlet(getContext());
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
