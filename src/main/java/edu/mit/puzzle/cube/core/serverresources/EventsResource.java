package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.events.Event;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import java.io.IOException;

public class EventsResource extends AbstractCubeResource {

    @Post
    public Representation handlePost(JsonRepresentation representation) throws JsonProcessingException {
        try {
            JSONObject obj = representation.getJsonObject();
            Event event = eventFactory.generate(obj.toString());

            eventProcessor.process(event);
            return new JsonRepresentation(MAPPER.writeValueAsString(ImmutableMap.of("processed",true)));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
