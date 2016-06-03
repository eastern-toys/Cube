package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.model.PostResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Post;

import java.io.IOException;

public class EventsResource extends AbstractCubeResource {

    @Post
    public PostResult handlePost(JsonRepresentation representation) throws JsonProcessingException {
        try {
            JSONObject obj = representation.getJsonObject();
            Event event = eventFactory.generate(obj.toString());

            eventProcessor.process(event);
            return PostResult.builder().setProcessed(true).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
