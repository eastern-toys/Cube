package edu.mit.puzzle.cube.core.permissions;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
public abstract class CubeRole {

    public static final String WRITING_TEAM_ROLE_NAME = "writingteam";
    public static final CubeRole WRITING_TEAM = new AutoValue_CubeRole(
            WRITING_TEAM_ROLE_NAME,
            ImmutableList.of(
                    new UsersPermission("*", PermissionAction.READ),
                    new UserRolesPermission("*", PermissionAction.READ),
                    new TeamsPermission("*", PermissionAction.READ),
                    new SubmissionsPermission("*", PermissionAction.READ, PermissionAction.UPDATE),
                    new VisibilitiesPermission("*", PermissionAction.READ),
                    new AnswersPermission()
            ));

    public static final String ADMIN_ROLE_NAME = "admin";
    public static final CubeRole ADMIN = new AutoValue_CubeRole(
            ADMIN_ROLE_NAME,
            ImmutableList.of(
                    new AllPermission()
            ));

    public static final ImmutableList<CubeRole> ALL_ROLES = ImmutableList.of(
            WRITING_TEAM,
            ADMIN
    );

    public static CubeRole create(String name) {
        return new AutoValue_CubeRole(name, ImmutableList.<CubePermission>of());
    }

    public abstract String getName();
    public abstract List<CubePermission> getPermissions();
}
