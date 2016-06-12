package edu.mit.puzzle.cube.core.permissions;

import com.google.common.collect.ImmutableList;

abstract class ActionLevelPermission extends CubePermission {
    private static final long serialVersionUID = 1L;

    ActionLevelPermission(String domain, PermissionAction[] actions) {
        super(String.format(
                "%s:%s",
                domain,
                PermissionAction.forWildcardString(ImmutableList.copyOf(actions))));
    }
}
