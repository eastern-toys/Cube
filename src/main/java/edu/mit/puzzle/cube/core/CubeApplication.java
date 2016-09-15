package edu.mit.puzzle.cube.core;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;
import edu.mit.puzzle.cube.core.environments.DevelopmentEnvironment;
import edu.mit.puzzle.cube.core.environments.ProductionEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.PeriodicTimerEvent;
import edu.mit.puzzle.cube.core.model.HintRequestStore;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.PuzzleStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.UserStore;
import edu.mit.puzzle.cube.core.serverresources.AbstractCubeResource;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.service.CorsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class CubeApplication extends Application {
    private static Logger LOGGER = LoggerFactory.getLogger(CubeApplication.class);

    private final SubmissionStore submissionStore;
    private final HuntStatusStore huntStatusStore;
    private final UserStore userStore;
    private final PuzzleStore puzzleStore;
    private final HintRequestStore hintRequestStore;
    private final CompositeEventProcessor eventProcessor;

    private final Service timingEventService;

    public CubeApplication(CubeConfig config) throws SQLException {
        CorsService corsService = new CorsService();
        corsService.setAllowedOrigins(config.getCorsAllowedOrigins());
        corsService.setAllowedCredentials(true);
        corsService.setAllowingAllRequestedHeaders(true);
        getServices().add(corsService);

        setStatusService(new CubeStatusService(corsService));

        HuntDefinition huntDefinition = HuntDefinition.forClassName(config.getHuntDefinitionClassName());

        ServiceEnvironment serviceEnvironment = null;
        switch (config.getServiceEnvironment()) {
        case DEVELOPMENT:
            serviceEnvironment = new DevelopmentEnvironment(huntDefinition);
            break;
        case PRODUCTION:
            serviceEnvironment = new ProductionEnvironment(config);
            break;
        default:
            LOGGER.error("Unimplemented service environment: " + config.getServiceEnvironment());
            System.exit(1);
        }

        ConnectionFactory connectionFactory = serviceEnvironment.getConnectionFactory();

        setupAuthentication(connectionFactory);

        eventProcessor = huntDefinition.generateCompositeEventProcessor();
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
        puzzleStore = new PuzzleStore(
                huntDefinition.getPuzzles()
        );
        hintRequestStore = new HintRequestStore(
                connectionFactory,
                huntDefinition,
                huntStatusStore,
                eventProcessor
        );

        huntDefinition.addToEventProcessor(
                eventProcessor,
                huntStatusStore
        );

        timingEventService = new AbstractScheduledService() {
            @Override
            protected void runOneIteration() throws Exception {
                try {
                    eventProcessor.process(PeriodicTimerEvent.builder().build());
                } catch (Exception e) {
                    LOGGER.error("Failure while processing periodic timer event", e);
                }
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

        DefaultSecurityManager securityManager = new DefaultSecurityManager(realm);

        // Disable Shiro session storage.
        final DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        final DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        securityManager.setSubjectDAO(subjectDAO);

        SecurityUtils.setSecurityManager(securityManager);
    }

    @Override
    public synchronized Restlet createInboundRoot() {
        // Put dependencies into the context so that the Resource handlers can access them.
        getContext().getAttributes().put(AbstractCubeResource.SUBMISSION_STORE_KEY, submissionStore);
        getContext().getAttributes().put(AbstractCubeResource.HUNT_STATUS_STORE_KEY, huntStatusStore);
        getContext().getAttributes().put(AbstractCubeResource.USER_STORE_KEY, userStore);
        getContext().getAttributes().put(AbstractCubeResource.PUZZLE_STORE_KEY, puzzleStore);
        getContext().getAttributes().put(AbstractCubeResource.HINT_REQUEST_STORE_KEY, hintRequestStore);
        getContext().getAttributes().put(AbstractCubeResource.EVENT_PROCESSOR_KEY, eventProcessor);

        return new CubeRestlet(getContext());
    }

    public static void main (String[] args) throws Exception {
        CubeConfig config = CubeConfig.readFromConfigJson();

        // Create a new Component.
        Component component = new Component();

        // Add a new HTTP server.
        component.getServers().add(Protocol.HTTP, config.getPort());

        // Attach this application.
        component.getDefaultHost().attach("", new CubeApplication(config));

        // Start the component.
        component.start();
    }

}
