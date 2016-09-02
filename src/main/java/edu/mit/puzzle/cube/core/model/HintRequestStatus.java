package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public enum HintRequestStatus {
    REQUESTED,
    ASSIGNED,
    ANSWERED,
    REJECTED;

    private static Set<HintRequestStatus> ASSIGNED_STATUSES =
            ImmutableSet.of(ASSIGNED, ANSWERED, REJECTED);
    private static Set<HintRequestStatus> TERMINAL_STATUSES = ImmutableSet.of(ANSWERED, REJECTED);

    public static HintRequestStatus getDefault() {
        return REQUESTED;
    }

    public boolean isAssigned() {
        return ASSIGNED_STATUSES.contains(this);
    }

    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }
}
