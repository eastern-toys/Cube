package edu.mit.puzzle.cube.core.permissions;

public class TeamsPermission extends InstanceLevelPermission {
    private static final long serialVersionUID = 1L;

    public TeamsPermission(String teamId, PermissionAction... actions) {
        super("teams", teamId, actions);
    }
}
