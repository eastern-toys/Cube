package edu.mit.puzzle.cube.core.permissions;

public class UsersPermission extends InstanceLevelPermission {
    private static final long serialVersionUID = 1L;

    public UsersPermission(String username, PermissionAction... actions) {
        super("users", username, actions);
    }
}
