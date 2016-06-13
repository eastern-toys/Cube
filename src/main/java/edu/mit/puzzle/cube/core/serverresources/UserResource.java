package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.base.Preconditions;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.UserRolesPermission;
import edu.mit.puzzle.cube.core.permissions.UsersPermission;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

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

        Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(new UsersPermission(id, PermissionAction.READ));

        User user = userStore.getUser(id);

        if (!subject.isPermitted(new UserRolesPermission(id, PermissionAction.READ))) {
            user = user.toBuilder()
                    .setRoles(null)
                    .build();
        }

        return user;
    }

    @Post
    public PostResult handlePost(User user) {
        String id = getId();

        Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(new UsersPermission(id, PermissionAction.UPDATE));
        if (user.getRoles() != null) {
            subject.checkPermission(new UserRolesPermission(id, PermissionAction.UPDATE));
        }

        if (user.getUsername() == null) {
            user = user.toBuilder()
                    .setUsername(id)
                    .build();
        } else {
            Preconditions.checkArgument(
                    user.getUsername().equals(id),
                    "Username did not match users route");
        }

        return PostResult.builder()
                .setUpdated(userStore.updateUser(user))
                .build();
    }
}
