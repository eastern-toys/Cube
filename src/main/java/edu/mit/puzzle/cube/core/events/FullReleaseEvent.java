package edu.mit.puzzle.cube.core.events;

import static com.google.common.base.Preconditions.checkNotNull;

public class FullReleaseEvent implements Event {

    public static final String EVENT_TYPE = "FullRelease";

    private final String puzzleId;

    public FullReleaseEvent(String puzzleId) {
        this.puzzleId = checkNotNull(puzzleId);
    }

    public String getPuzzleId() {
        return puzzleId;
    }
}
