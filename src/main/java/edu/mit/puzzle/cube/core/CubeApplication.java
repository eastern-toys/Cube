package edu.mit.puzzle.cube.core;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.DevelopmentEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.CoreEventFactory;
import edu.mit.puzzle.cube.core.events.EventFactory;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.serverresources.*;
import edu.mit.puzzle.cube.huntimpl.linearexample.LinearExampleHuntDefinition;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import java.sql.SQLException;

public class CubeApplication extends Application {

    private final SubmissionStore submissionStore;
    private final HuntStatusStore huntStatusStore;
    private final EventFactory eventFactory;
    private final CompositeEventProcessor eventProcessor;

    public CubeApplication() throws SQLException {
        HuntDefinition huntDefinition = new LinearExampleHuntDefinition();
        ServiceEnvironment serviceEnvironment = new DevelopmentEnvironment(huntDefinition);

        ConnectionFactory connectionFactory = serviceEnvironment.getConnectionFactory();

        eventFactory = new CoreEventFactory();
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

        huntDefinition.addToEventProcessor(
                eventProcessor,
                huntStatusStore
        );
    }

    @Override
    public synchronized Restlet createInboundRoot() {
        // Create a router Restlet that routes each call to a new instance of HelloWorldResource.
        Router router = new Router(getContext());

        //Put dependencies into the router context so that the Resource handlers can access them
        router.getContext().getAttributes().put(AbstractCubeResource.SUBMISSION_STORE_KEY, submissionStore);
        router.getContext().getAttributes().put(AbstractCubeResource.HUNT_STATUS_STORE_KEY, huntStatusStore);
        router.getContext().getAttributes().put(AbstractCubeResource.EVENT_FACTORY_KEY, eventFactory);
        router.getContext().getAttributes().put(AbstractCubeResource.EVENT_PROCESSOR_KEY, eventProcessor);

        //Define routes
        router.attach("/submissions", SubmissionsResource.class);
        router.attach("/submissions/{id}", SubmissionResource.class);
        router.attach("/visibilities", VisibilitiesResource.class);
        router.attach("/visibilities/{teamId}/{puzzleId}", VisibilityResource.class);
        router.attach("/events", EventsResource.class);
        router.attach("/teams/{id}", TeamResource.class);

        return router;
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
