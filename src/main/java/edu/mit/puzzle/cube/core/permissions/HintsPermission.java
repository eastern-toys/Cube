package edu.mit.puzzle.cube.core.permissions;

public class HintsPermission extends InstanceLevelPermission {
    private static final long serialVersionUID = 1L;

    public HintsPermission(String teamId, PermissionAction... actions) {
        super("hints", teamId, actions);
    }
}
