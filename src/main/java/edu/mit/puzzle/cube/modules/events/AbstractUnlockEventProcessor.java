package edu.mit.puzzle.cube.modules.events;

import edu.mit.puzzle.cube.core.events.*;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractUnlockEventProcessor implements EventProcessor {

    protected final HuntStatusStore huntStatusStore;

    public AbstractUnlockEventProcessor(
            HuntStatusStore huntStatusStore
    ) {
        this.huntStatusStore = checkNotNull(huntStatusStore);
    }


    public void process(Event event) {
        if (HuntStartEvent.class.isInstance(event)) {
            String runId = ((HuntStartEvent) event).getRunId();
            processHuntStart(runId);
        } else if (VisibilityChangeEvent.class.isInstance(event)) {
            String teamId = ((VisibilityChangeEvent) event).getTeamId();
            processUpdates(teamId);
        } else if (TimingUpdateEvent.class.isInstance(event)) {
            String runId = ((TimingUpdateEvent) event).getRunId();
            for (String teamId : huntStatusStore.getTeamIds(runId)) {
                processUpdates(teamId);
            }
        } else if (FullReleaseEvent.class.isInstance(event)) {
            String runId = ((FullReleaseEvent) event).getRunId();
            String puzzleId = ((FullReleaseEvent) event).getPuzzleId();
            for (String teamId : huntStatusStore.getTeamIds(runId)) {
                huntStatusStore.setVisibility(
                        teamId,
                        puzzleId,
                        "UNLOCKED",
                        false
                );
            }
        }
    }

    protected void processHuntStart(String runId) {
        boolean start = huntStatusStore.recordHuntRunStart(runId);
        if (!start) {
            return;
        }

        Map<String,Object> startProperties = getInitialPropertiesForTeam();

        Set<String> teamIds = huntStatusStore.getTeamIds(runId);
        for (String teamId : teamIds) {
            for (Map.Entry<String,Object> property : startProperties.entrySet()) {
                huntStatusStore.setTeamProperty(teamId, property.getKey(), property.getValue());
            }
        }

        for (String teamId : teamIds) {
            processUpdates(teamId);
        }
    }

    protected abstract Map<String, Object> getInitialPropertiesForTeam();

    protected void processUpdates(String teamId) {
        String runId = huntStatusStore.getRunForTeam(teamId);
        Map<String,Object> currentHuntRunProperties = huntStatusStore.getHuntRunProperties(runId);
        Map<String,Object> currentProperties = huntStatusStore.getTeamProperties(teamId);
        Map<String,String> currentVisibilities = huntStatusStore.getVisibilitiesForTeam(teamId);

        AbstractUnlockUpdateCalculator abstractUnlockUpdateCalculator = generateUnlockUpdateCalculator(
                currentHuntRunProperties,
                currentProperties,
                currentVisibilities
        );

        Map<String,Object> updatedProperties = abstractUnlockUpdateCalculator.getTeamPropertyUpdates();
        boolean anyPropertyUpdates = false;
        for (Map.Entry<String,Object> updatedProperty : updatedProperties.entrySet()) {
            boolean update = huntStatusStore.setTeamProperty(
                    teamId, updatedProperty.getKey(), updatedProperty.getValue());
            anyPropertyUpdates = anyPropertyUpdates || update;
        }
        if (anyPropertyUpdates) {
            processUpdates(teamId);
        }

        Map<String,String> updatedVisibilities = abstractUnlockUpdateCalculator.getTeamVisibilityUpdates();
        boolean anyVisibilityUpdates = false;
        for (Map.Entry<String,String> updatedVisibility : updatedVisibilities.entrySet()) {
            boolean update = huntStatusStore.setVisibility(
                    teamId, updatedVisibility.getKey(), updatedVisibility.getValue(), false);
            anyVisibilityUpdates = anyVisibilityUpdates || update;
        }
        if (anyVisibilityUpdates) {
            processUpdates(teamId);
        }
    }

    protected abstract AbstractUnlockUpdateCalculator generateUnlockUpdateCalculator(
            Map<String,Object> currentHuntRunProperties,
            Map<String,Object> currentTeamUnlockProperties,
            Map<String,String> currentTeamVisibilities
    );
}
