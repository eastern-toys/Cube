package edu.mit.puzzle.cube.huntimpl.linearexample;

import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.events.AbstractUnlockUpdateCalculator;

import java.util.Map;
import java.util.function.Supplier;

public class LinearExampleUnlockUpdateCalculator extends AbstractUnlockUpdateCalculator {

    public LinearExampleUnlockUpdateCalculator(
            VisibilityStatusSet visibilityStatusSet,
            Map<String, Object> currentHuntRunProperties,
            Map<String, Object> currentTeamUnlockProperties,
            Map<String, String> currentTeamVisibilities
    ) {
        super(
                visibilityStatusSet,
                currentHuntRunProperties,
                currentTeamUnlockProperties,
                currentTeamVisibilities
        );
    }

    ImmutableMap<String,Supplier<Boolean>> PUZZLES_BECOME_UNLOCKED_WHEN =
            ImmutableMap.<String,Supplier<Boolean>>builder()
                    .put("puzzle1", () -> whenHuntStarts())
                    .put("puzzle2", () -> whenSolved("puzzle1"))
                    .put("puzzle3", () -> whenSolved("puzzle2"))
                    .put("puzzle4", () -> whenSolved("puzzle3"))
                    .put("puzzle5", () -> whenSolved("puzzle4"))
                    .put("puzzle6", () -> whenSolved("puzzle5"))
                    .put("puzzle7", () -> whenSolved("puzzle6"))
                    .build();

    protected Map<String,Supplier<Boolean>> getMapOfWhenStatusOccurs(String status) {
        if (status.equals("UNLOCKED")) {
            return PUZZLES_BECOME_UNLOCKED_WHEN;
        } else {
            return ImmutableMap.of();
        }
    }


    @Override
    public Map<String, Object> getTeamPropertyUpdates() {
        return ImmutableMap.of();
    }


}
