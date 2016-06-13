package edu.mit.puzzle.cube.core.permissions;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
public abstract class CubeRole {

    public static final CubeRole WRITING_TEAM = new AutoValue_CubeRole(
            "writingteam",
            ImmutableList.of(
                    new UsersPermission("*", PermissionAction.READ),
                    new UserRolesPermission("*", PermissionAction.READ),
                    new TeamsPermission("*", PermissionAction.READ),
                    new SubmissionsPermission("*", PermissionAction.READ, PermissionAction.UPDATE),
                    new VisibilitiesPermission("*", PermissionAction.READ)
            ));

    public static final CubeRole ADMIN = new AutoValue_CubeRole(
            "admin",
            ImmutableList.of(
                    new AllPermission()
            ));

    public abstract String getName();
    public abstract List<CubePermission> getPermissions();
}
