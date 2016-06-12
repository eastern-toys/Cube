package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.permissions.EventsPermission;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Post;


public class EventsResource extends AbstractCubeResource {

    @Post
    public PostResult handlePost(Event event) {
        SecurityUtils.getSubject().checkPermission(
                new EventsPermission(event, PermissionAction.CREATE));
        eventProcessor.process(event);
        return PostResult.builder().setProcessed(true).build();
    }
}
