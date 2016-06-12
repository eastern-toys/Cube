package edu.mit.puzzle.cube.core.permissions;

import org.apache.shiro.authz.permission.WildcardPermission;

public abstract class CubePermission extends WildcardPermission {
    private static final long serialVersionUID = 1L;

    private final String wildcardString;

    CubePermission(String wildcardString) {
        super(wildcardString);
        this.wildcardString = wildcardString;
    }

    public String getWildcardString() {
        return wildcardString;
    }
}
