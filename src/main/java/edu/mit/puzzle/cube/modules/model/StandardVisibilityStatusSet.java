package edu.mit.puzzle.cube.modules.model;

import com.google.common.collect.ImmutableSet;

import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;

import java.util.Set;

public class StandardVisibilityStatusSet implements VisibilityStatusSet {

    private static final Set<String> ALL_STATUSES = ImmutableSet.of(
            "INVISIBLE", "VISIBLE", "UNLOCKED", "SOLVED");
    private static final Set<String> SUBMISSION_STATUSES = ImmutableSet.of("UNLOCKED");

    @Override
    public Set<String> getAllowedStatuses() {
        return ALL_STATUSES;
    }

    @Override
    public String getDefaultVisibilityStatus() {
        return "INVISIBLE";
    }

    @Override
    public boolean isAllowedStatus(String status) {
        return ALL_STATUSES.contains(status);
    }

    @Override
    public boolean allowsSubmissions(String status) {
        return SUBMISSION_STATUSES.contains(status);
    }

    @Override
    public Set<String> getAllowedAntecedents(String status) {
        switch(status) {
            case "INVISIBLE":   return ImmutableSet.of();
            case "VISIBLE":     return ImmutableSet.of("INVISIBLE");
            case "UNLOCKED":    return ImmutableSet.of("INVISIBLE","VISIBLE");
            case "SOLVED":      return ImmutableSet.of("UNLOCKED");
            default:            throw new IllegalArgumentException();
        }
    }
}
