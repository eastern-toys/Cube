package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.model.PostResult;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Post;


public class EventsResource extends AbstractCubeResource {

    @Post
    public PostResult handlePost(Event event) throws JsonProcessingException {
        SecurityUtils.getSubject().checkPermission("events:create:" + event.getType());
        eventProcessor.process(event);
        return PostResult.builder().setProcessed(true).build();
    }
}
