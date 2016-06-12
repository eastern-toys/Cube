package edu.mit.puzzle.cube.core.permissions;

import com.google.common.collect.ImmutableList;

abstract class InstanceLevelPermission extends CubePermission {
    private static final long serialVersionUID = 1L;

    InstanceLevelPermission(String domain, String instance, PermissionAction[] actions) {
        super(String.format(
                "%s:%s:%s",
                domain,
                PermissionAction.forWildcardString(ImmutableList.copyOf(actions)),
                instance));
    }
}
