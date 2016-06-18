package edu.mit.puzzle.cube.core.permissions;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AutoValue
public abstract class RolesAndInstanceLevelPermissions {
    public static final RolesAndInstanceLevelPermissions NONE = new AutoValue_RolesAndInstanceLevelPermissions(
            ImmutableList.<CubeRole>of(),
            ImmutableList.<CubePermission>of());

    public static RolesAndInstanceLevelPermissions forUser(User user) {
        RolesAndInstanceLevelPermissions rolesAndPermissions = RolesAndInstanceLevelPermissions.NONE;
        if (user.getTeamId() != null) {
            rolesAndPermissions = RolesAndInstanceLevelPermissions.forSolvingTeam(user.getTeamId());
        }
        if (user.getRoles() != null) {
            for (String roleName : user.getRoles()) {
                RolesAndInstanceLevelPermissions newRolesAndPermissions = null;
                switch (roleName) {
                case CubeRole.WRITING_TEAM_ROLE_NAME:
                    newRolesAndPermissions =
                            RolesAndInstanceLevelPermissions.forWritingTeam(user.getUsername());
                    break;
                case CubeRole.ADMIN_ROLE_NAME:
                    newRolesAndPermissions = RolesAndInstanceLevelPermissions.forAdmin();
                    break;
                default:
                    // Roles without instance-level permissions may be defined in the database only,
                    // they don't necessarily need to be modeled in this Java package.
                    newRolesAndPermissions = new AutoValue_RolesAndInstanceLevelPermissions(
                            ImmutableList.of(CubeRole.create(roleName)),
                            ImmutableList.<CubePermission>of());
                }
                if (newRolesAndPermissions != null) {
                    rolesAndPermissions = rolesAndPermissions.merge(newRolesAndPermissions);
                }
            }
        }
        return rolesAndPermissions;
    }

    public static RolesAndInstanceLevelPermissions forSolvingTeam(String teamId) {
        return new AutoValue_RolesAndInstanceLevelPermissions(
                ImmutableList.<CubeRole>of(),
                ImmutableList.of(
                        new UsersPermission(teamId, PermissionAction.READ),
                        new TeamsPermission(teamId, PermissionAction.READ),
                        new SubmissionsPermission(teamId, PermissionAction.CREATE, PermissionAction.READ),
                        new VisibilitiesPermission(teamId, PermissionAction.READ)
                ));
    }

    public static RolesAndInstanceLevelPermissions forWritingTeam(String username) {
        return new AutoValue_RolesAndInstanceLevelPermissions(
                ImmutableList.of(CubeRole.WRITING_TEAM),
                ImmutableList.of(
                        new UsersPermission(username, PermissionAction.READ, PermissionAction.UPDATE)
                ));
    }

    public static RolesAndInstanceLevelPermissions forAdmin() {
        return new AutoValue_RolesAndInstanceLevelPermissions(
                ImmutableList.of(CubeRole.ADMIN),
                ImmutableList.<CubePermission>of());
    }

    public RolesAndInstanceLevelPermissions merge(RolesAndInstanceLevelPermissions other) {
        Set<CubeRole> roles = new HashSet<>(getRoles());
        roles.addAll(other.getRoles());

        Set<CubePermission> permissions = new HashSet<>(getInstanceLevelPermissions());
        permissions.addAll(other.getInstanceLevelPermissions());

        return new AutoValue_RolesAndInstanceLevelPermissions(
                ImmutableList.copyOf(roles),
                ImmutableList.copyOf(permissions));
    }

    public abstract List<CubeRole> getRoles();
    public abstract List<CubePermission> getInstanceLevelPermissions();
}
