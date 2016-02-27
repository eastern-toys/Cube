package edu.mit.puzzle.cube.core.events;

import static com.google.common.base.Preconditions.checkNotNull;

public class FullReleaseEvent extends Event {

    public static final String EVENT_TYPE = "FullRelease";

    private final String runId;
    private final String puzzleId;

    public FullReleaseEvent(String runId, String puzzleId) {
        super(EVENT_TYPE);
        this.runId = checkNotNull(runId);
        this.puzzleId = checkNotNull(puzzleId);
    }

    public String getRunId() {
        return runId;
    }

    public String getPuzzleId() {
        return puzzleId;
    }
}
