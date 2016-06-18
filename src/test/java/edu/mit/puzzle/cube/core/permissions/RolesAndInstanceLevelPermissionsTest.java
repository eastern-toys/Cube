package edu.mit.puzzle.cube.core.permissions;

import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.model.User;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class RolesAndInstanceLevelPermissionsTest {

    @Test
    public void mergeWithNone() {
        RolesAndInstanceLevelPermissions merged = RolesAndInstanceLevelPermissions.NONE.merge(
                RolesAndInstanceLevelPermissions.forAdmin());
        assertThat(merged).isEqualTo(RolesAndInstanceLevelPermissions.forAdmin());
    }

    @Test
    public void mergeWritingTeamAndAdmin() {
        RolesAndInstanceLevelPermissions forWritingTeam =
                RolesAndInstanceLevelPermissions.forWritingTeam("writingteamuser");
        RolesAndInstanceLevelPermissions forAdmin =
                RolesAndInstanceLevelPermissions.forAdmin();
        RolesAndInstanceLevelPermissions merged = forWritingTeam.merge(forAdmin);

        assertThat(merged.getRoles()).containsExactly(CubeRole.WRITING_TEAM, CubeRole.ADMIN);

        assertThat(merged.getInstanceLevelPermissions()).containsAllIn(
                forWritingTeam.getInstanceLevelPermissions());
        assertThat(merged.getInstanceLevelPermissions()).containsAllIn(
                forAdmin.getInstanceLevelPermissions());
    }

    @Test
    public void forUser_solvingTeam() {
        User user = User.builder()
                .setUsername("solvingteam")
                .setTeamId("solvingteam")
                .build();
        RolesAndInstanceLevelPermissions rolesAndPermissions = RolesAndInstanceLevelPermissions.forUser(user);
        assertThat(rolesAndPermissions.getRoles()).isEmpty();
        assertThat(rolesAndPermissions.getInstanceLevelPermissions()).contains(
                new UsersPermission("solvingteam", PermissionAction.READ));
    }

    @Test
    public void forUser_writingTeam() {
        User user = User.builder()
                .setUsername("writingteamuser")
                .setRoles(ImmutableList.of("writingteam"))
                .build();
        RolesAndInstanceLevelPermissions rolesAndPermissions = RolesAndInstanceLevelPermissions.forUser(user);
        assertThat(rolesAndPermissions.getRoles()).containsExactly(CubeRole.WRITING_TEAM);
        assertThat(rolesAndPermissions.getInstanceLevelPermissions()).contains(
                new UsersPermission("writingteamuser", PermissionAction.READ, PermissionAction.UPDATE));
    }

    @Test
    public void forUser_unmodeledRole() {
        User user = User.builder()
                .setUsername("specialuser")
                .setRoles(ImmutableList.of("special"))
                .build();
        RolesAndInstanceLevelPermissions rolesAndPermissions = RolesAndInstanceLevelPermissions.forUser(user);
        assertThat(rolesAndPermissions.getRoles()).containsExactly(CubeRole.create("special"));
        assertThat(rolesAndPermissions.getInstanceLevelPermissions()).isEmpty();
    }
}
