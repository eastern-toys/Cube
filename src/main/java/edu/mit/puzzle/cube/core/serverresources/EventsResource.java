package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

import edu.mit.puzzle.cube.core.events.Event;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

public class EventsResource extends AbstractCubeResource {

    @Post
    public Representation handlePost(Event event) throws JsonProcessingException {
        eventProcessor.process(event);
        return new JsonRepresentation(MAPPER.writeValueAsString(ImmutableMap.of("processed",true)));
    }
}
