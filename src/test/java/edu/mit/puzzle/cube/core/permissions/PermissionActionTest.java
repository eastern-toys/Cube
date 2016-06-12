package edu.mit.puzzle.cube.core.permissions;

import com.google.common.collect.ImmutableList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.truth.Truth.assertThat;

public class PermissionActionTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void forWildcardString() {
        assertThat(
                PermissionAction.forWildcardString(ImmutableList.of(PermissionAction.READ)))
        .isEqualTo("read");

        assertThat(
                PermissionAction.forWildcardString(
                        ImmutableList.of(PermissionAction.READ, PermissionAction.UPDATE)))
        .isEqualTo("read,update");

        assertThat(
                PermissionAction.forWildcardString(ImmutableList.of(PermissionAction.ANY)))
        .isEqualTo("*");
    }

    @Test
    public void forWildcardString_noActions() {
        exception.expect(IllegalArgumentException.class);
        PermissionAction.forWildcardString(ImmutableList.<PermissionAction>of());
    }

    @Test
    public void forWildcardString_anyWithOtherActions() {
        exception.expect(IllegalArgumentException.class);
        PermissionAction.forWildcardString(
                ImmutableList.of(PermissionAction.ANY, PermissionAction.READ));
    }

}
