package edu.mit.puzzle.cube.core.permissions;

public class AnswersPermission extends ActionLevelPermission {
    private static final long serialVersionUID = 1L;

    public AnswersPermission() {
        super("answers", new PermissionAction[]{PermissionAction.READ});
    }
}
