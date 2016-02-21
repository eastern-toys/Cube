package edu.mit.puzzle.cube.modules.events;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractUnlockUpdateCalculator {

    protected final VisibilityStatusSet visibilityStatusSet;
    protected final ImmutableMap<String,Object> currentHuntRunProperties;
    protected final ImmutableMap<String,Object> currentTeamUnlockProperties;
    protected final ImmutableMap<String,String> currentTeamVisibilities;




    public AbstractUnlockUpdateCalculator(
            VisibilityStatusSet visibilityStatusSet,
            Map<String, Object> currentHuntRunProperties,
            Map<String, Object> currentTeamUnlockProperties,
            Map<String, String> currentTeamVisibilities
    ) {
        this.visibilityStatusSet = checkNotNull(visibilityStatusSet);
        this.currentHuntRunProperties = ImmutableMap.copyOf(currentHuntRunProperties);
        this.currentTeamUnlockProperties = ImmutableMap.copyOf(currentTeamUnlockProperties);
        this.currentTeamVisibilities = ImmutableMap.copyOf(currentTeamVisibilities);
    }

    private boolean canBecome(String status, String puzzleId) {
        return visibilityStatusSet.getAllowedAntecedents(status)
                .contains(currentTeamVisibilities.get(puzzleId));
    }

    private boolean shouldBecome(String status, String puzzleId) {
        if (!canBecome(status, puzzleId)) {
            return false;
        }

        Map<String,Supplier<Boolean>> whenMap = getMapOfWhenStatusOccurs(status);
        if (!whenMap.containsKey(puzzleId)) {
            return false;
        }

        return whenMap.get(puzzleId).get();
    }

    protected abstract Map<String,Supplier<Boolean>> getMapOfWhenStatusOccurs(String status);



    public Map<String,String> getTeamVisibilityUpdates() {
        Map<String,String> visibilityUpdates = Maps.newHashMap();

        for (String status : visibilityStatusSet.getAllowedStatuses()) {
            for (String puzzleId : currentTeamVisibilities.keySet()) {

                if (shouldBecome(status, puzzleId) && !visibilityUpdates.containsKey(puzzleId)) {
                    visibilityUpdates.put(puzzleId, status);
                }

            }
        }

        return ImmutableMap.copyOf(visibilityUpdates);
    }

    public abstract Map<String,Object> getTeamPropertyUpdates();


    protected boolean whenUnlocked(String puzzleId) {
        return currentTeamVisibilities.get(puzzleId).equals("UNLOCKED");
    }

    protected boolean whenSolved(String puzzleId) {
        return currentTeamVisibilities.get(puzzleId).equals("SOLVED");
    }

    protected boolean whenHuntStarts() {
        Optional<Instant> huntStartTimestamp = Optional.ofNullable(
                (Instant) currentHuntRunProperties.get("startTimestamp")
        );
        return huntStartTimestamp.isPresent() &&
                huntStartTimestamp.get().isBefore(Instant.now());
    }

}
