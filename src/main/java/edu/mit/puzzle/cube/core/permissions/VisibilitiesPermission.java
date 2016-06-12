package edu.mit.puzzle.cube.core.permissions;

public class VisibilitiesPermission extends InstanceLevelPermission {
    private static final long serialVersionUID = 1L;

    public VisibilitiesPermission(String teamId, PermissionAction... actions) {
        super("visibilities", teamId, actions);
    }
}
