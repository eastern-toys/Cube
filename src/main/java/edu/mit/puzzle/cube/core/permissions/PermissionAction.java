package edu.mit.puzzle.cube.core.permissions;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.stream.Collectors;

public enum PermissionAction {
    ANY("*"),
    CREATE("create"),
    READ("read"),
    UPDATE("update");

    static String forWildcardString(List<PermissionAction> actions) {
        Preconditions.checkArgument(
                actions.size() > 0,
                "At least one action must be specified");
        Preconditions.checkArgument(
                !actions.contains(ANY) || actions.size() == 1,
                "ANY action should not be combined with other action values");
        return Joiner.on(",").join(
                actions.stream()
                .map(PermissionAction::getToken)
                .collect(Collectors.toList()));
    }

    private PermissionAction(String token) {
        this.token = token;
    }

    String getToken() {
        return token;
    }

    private final String token;
}
