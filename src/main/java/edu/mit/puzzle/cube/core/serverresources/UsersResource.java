package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.base.Preconditions;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.User;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Post;

import java.util.ArrayList;
import java.util.List;

public class UsersResource extends AbstractCubeResource {

    @Post
    public PostResult handlePost(User user) {
        SecurityUtils.getSubject().checkPermission("users:create");
        Preconditions.checkArgument(
                !user.getUsername().isEmpty(),
                "The username must be non-empty"
        );
        Preconditions.checkArgument(
                user.getPassword() != null && !user.getPassword().isEmpty(),
                "A password must be provided when creating a user"
        );

        userStore.addUser(user);

        List<String> instanceLevelPermissions = new ArrayList<>();
        instanceLevelPermissions.add("userinfo:*:" + user.getUsername());
        userStore.addUserPermissions(user.getUsername(), instanceLevelPermissions);

        return PostResult.builder().setCreated(true).build();
    }
}
