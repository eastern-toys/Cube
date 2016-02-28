package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mit.puzzle.cube.core.events.EventFactory;
import edu.mit.puzzle.cube.core.events.GenericEventProcessor;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public abstract class AbstractCubeResource extends ServerResource {

    protected static ObjectMapper MAPPER = new ObjectMapper();

    public static String SUBMISSION_STORE_KEY = "SUBMISSION_STORE";
    public static String HUNT_STATUS_STORE_KEY = "HUNT_STATUS_STORE";
    public static String EVENT_FACTORY_KEY = "EVENT_FACTORY";
    public static String EVENT_PROCESSOR_KEY = "EVENT_PROCESSOR";

    protected SubmissionStore submissionStore;
    protected HuntStatusStore huntStatusStore;
    protected EventFactory eventFactory;
    protected GenericEventProcessor eventProcessor;

    public AbstractCubeResource() {
    }

    public void doInit() {
        this.submissionStore = (SubmissionStore) getContext().getAttributes().get(SUBMISSION_STORE_KEY);
        this.huntStatusStore = (HuntStatusStore) getContext().getAttributes().get(HUNT_STATUS_STORE_KEY);
        this.eventFactory = (EventFactory) getContext().getAttributes().get(EVENT_FACTORY_KEY);
        this.eventProcessor = (GenericEventProcessor) getContext().getAttributes().get(EVENT_PROCESSOR_KEY);
    }

    @Get("json")
    public Representation handleGetRequest() {
        try {
            String resultJson = handleGet();
            if (resultJson != null) {
                return new JsonRepresentation(resultJson);
            } else {
                return null;
            }
        } catch (JsonProcessingException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "Internal error");
            return null;
        }
    }

    protected abstract String handleGet() throws JsonProcessingException;

    @Post("json")
    public Representation handlePostRequest(JsonRepresentation representation) {
        try {
            String resultJson = handlePost(representation);
            if (resultJson != null) {
                return new JsonRepresentation(resultJson);
            } else {
                return null;
            }
        } catch (JsonProcessingException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "Internal error");
            return null;
        }
    }

    protected abstract String handlePost(JsonRepresentation representation) throws JsonProcessingException;

}
