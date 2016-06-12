package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.UsersPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;

public class UserResource extends AbstractCubeResource {

    private String getId() {
        String idString = (String) getRequest().getAttributes().get("id");
        if (idString == null) {
            throw new IllegalArgumentException("id must be specified");
        }
        return idString;
    }

    @Get
    public User handleGet() {
        String id = getId();
        SecurityUtils.getSubject().checkPermission(
                new UsersPermission(id, PermissionAction.READ));
        return userStore.getUser(id);
    }
}
