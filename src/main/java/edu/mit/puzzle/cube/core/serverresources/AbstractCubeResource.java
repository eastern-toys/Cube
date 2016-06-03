package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mit.puzzle.cube.core.events.*;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import org.restlet.resource.ServerResource;

public abstract class AbstractCubeResource extends ServerResource {

    protected static ObjectMapper MAPPER = new ObjectMapper();

    public static String SUBMISSION_STORE_KEY = "SUBMISSION_STORE";
    public static String HUNT_STATUS_STORE_KEY = "HUNT_STATUS_STORE";
    public static String EVENT_FACTORY_KEY = "EVENT_FACTORY";
    public static String EVENT_PROCESSOR_KEY = "EVENT_PROCESSOR";

    protected SubmissionStore submissionStore;
    protected HuntStatusStore huntStatusStore;
    protected EventProcessor<Event> eventProcessor;

    public AbstractCubeResource() {
    }

    public void doInit() {
        this.submissionStore = (SubmissionStore) getContext().getAttributes().get(SUBMISSION_STORE_KEY);
        this.huntStatusStore = (HuntStatusStore) getContext().getAttributes().get(HUNT_STATUS_STORE_KEY);
        this.eventProcessor = (EventProcessor<Event>) getContext().getAttributes().get(EVENT_PROCESSOR_KEY);
    }
}
