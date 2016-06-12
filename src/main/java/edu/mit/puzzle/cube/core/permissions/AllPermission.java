package edu.mit.puzzle.cube.core.permissions;

public class AllPermission extends CubePermission {
    private static final long serialVersionUID = 1L;

    public AllPermission() {
        super("*");
    }
}
