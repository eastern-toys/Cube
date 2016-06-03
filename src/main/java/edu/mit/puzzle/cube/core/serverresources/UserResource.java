package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.model.User;

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
        // TODO: introduce a real users table, instead of assuming all users exist and have all
        // permissions.
        return User.builder()
                .setUsername(id)
                .setPermissions(ImmutableList.of("*"))
                .build();
    }
}
