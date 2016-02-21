package edu.mit.puzzle.cube.core.events;

import static com.google.common.base.Preconditions.checkNotNull;

public class FullReleaseEvent implements Event{

    private final String runId;
    private final String puzzleId;

    public FullReleaseEvent(String runId, String puzzleId) {
        this.runId = checkNotNull(runId);
        this.puzzleId = checkNotNull(puzzleId);
    }

    public String getRunId() {
        return runId;
    }

    public String getPuzzleId() {
        return puzzleId;
    }

    @Override
    public boolean isExternallyInitiated() {
        return true;
    }

}
