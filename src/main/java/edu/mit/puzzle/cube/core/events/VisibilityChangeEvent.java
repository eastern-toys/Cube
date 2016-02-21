package edu.mit.puzzle.cube.core.events;

import edu.mit.puzzle.cube.core.model.Submission;

import static com.google.common.base.Preconditions.checkNotNull;

public class VisibilityChangeEvent implements Event {

    private String teamId;
    private String puzzleId;
    private String visibilityStatus;
    private boolean isExternallyInitiated;

    public VisibilityChangeEvent(
            String teamId,
            String puzzleId,
            String visibilityStatus,
            boolean isExternallyInitiated
    ) {
        this.teamId = checkNotNull(teamId);
        this.puzzleId = checkNotNull(puzzleId);
        this.visibilityStatus = checkNotNull(visibilityStatus);
        this.isExternallyInitiated = isExternallyInitiated;
    }

    @Override
    public boolean isExternallyInitiated() {
        return this.isExternallyInitiated;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getPuzzleId() {
        return puzzleId;
    }

    public String getVisibilityStatus() {
        return visibilityStatus;
    }
}
