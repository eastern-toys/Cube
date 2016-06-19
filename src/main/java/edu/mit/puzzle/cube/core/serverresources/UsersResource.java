package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.base.Preconditions;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.core.model.Users;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.UserRolesPermission;
import edu.mit.puzzle.cube.core.permissions.UsersPermission;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.util.List;
import java.util.stream.Collectors;

public class UsersResource extends AbstractCubeResource {

    @Get
    public Users handleGet() {
        Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(new UsersPermission("*", PermissionAction.READ));

        List<User> users = userStore.getAllUsers();

        if (!subject.isPermitted(new UserRolesPermission("*", PermissionAction.READ))) {
            users = users.stream()
                    .map(user -> user.toBuilder().setRoles(null).build())
                    .collect(Collectors.toList());
        }

        return Users.builder()
                .setUsers(users)
                .build();
    }

    @Post
    public PostResult handlePost(User user) {
        SecurityUtils.getSubject().checkPermission(
                new UsersPermission(user.getUsername(), PermissionAction.CREATE));
        Preconditions.checkArgument(
                !user.getUsername().isEmpty(),
                "The username must be non-empty"
        );
        Preconditions.checkArgument(
                user.getPassword() != null && !user.getPassword().isEmpty(),
                "A password must be provided when creating a user"
        );
        userStore.addUser(user);
        return PostResult.builder().setCreated(true).build();
    }
}
