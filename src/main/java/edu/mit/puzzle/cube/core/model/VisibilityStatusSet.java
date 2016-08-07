package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public interface VisibilityStatusSet {

    public Set<String> getAllowedStatuses();

    public String getDefaultVisibilityStatus();

    public boolean isAllowedStatus(String status);

    public boolean allowsSubmissions(String status);

    public Set<String> getAllowedAntecedents(String status);

    default Set<String> getAllowedSuccessors(String status) {
        if (!getAllowedStatuses().contains(status)) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (String possibleSuccessor : getAllowedStatuses()) {
            if (getAllowedAntecedents(possibleSuccessor).contains(status)) {
                builder.add(possibleSuccessor);
            }
        }
        return builder.build();
    }
}
