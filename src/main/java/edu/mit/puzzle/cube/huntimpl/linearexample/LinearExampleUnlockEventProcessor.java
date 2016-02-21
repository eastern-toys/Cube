package edu.mit.puzzle.cube.huntimpl.linearexample;

import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.modules.events.AbstractUnlockEventProcessor;
import edu.mit.puzzle.cube.modules.events.AbstractUnlockUpdateCalculator;

import java.util.Map;

public class LinearExampleUnlockEventProcessor extends AbstractUnlockEventProcessor {

    public LinearExampleUnlockEventProcessor(HuntStatusStore huntStatusStore) {
        super(huntStatusStore);
    }

    @Override
    protected Map<String, Object> getInitialPropertiesForTeam() {
        return ImmutableMap.of();
    }

    @Override
    protected AbstractUnlockUpdateCalculator generateUnlockUpdateCalculator(
            Map<String, Object> currentHuntRunProperties,
            Map<String, Object> currentTeamUnlockProperties,
            Map<String, String> currentTeamVisibilities
    ) {
        return new LinearExampleUnlockUpdateCalculator(
                huntStatusStore.getVisibilityStatusSet(),
                currentHuntRunProperties,
                currentTeamUnlockProperties,
                currentTeamVisibilities
        );
    }
}
