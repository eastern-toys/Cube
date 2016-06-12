package edu.mit.puzzle.cube.core.permissions;

public class SubmissionsPermission extends InstanceLevelPermission {
    private static final long serialVersionUID = 1L;

    public SubmissionsPermission(String teamId, PermissionAction... actions) {
        super("submissions", teamId, actions);
    }
}
