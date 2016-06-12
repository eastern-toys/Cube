package edu.mit.puzzle.cube.core.permissions;

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

}
