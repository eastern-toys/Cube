package edu.mit.puzzle.cube.core.permissions;

public class UserRolesPermission extends InstanceLevelPermission {
    private static final long serialVersionUID = 1L;

    public UserRolesPermission(String username, PermissionAction... actions) {
        super("userroles", username, actions);
    }
}
