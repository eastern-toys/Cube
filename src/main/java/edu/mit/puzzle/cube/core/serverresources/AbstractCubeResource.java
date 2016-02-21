package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public abstract class AbstractCubeResource extends ServerResource {

    protected static ObjectMapper MAPPER = new ObjectMapper();

    public AbstractCubeResource() {
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
