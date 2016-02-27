package edu.mit.puzzle.cube.core.events;

import static com.google.common.base.Preconditions.checkNotNull;

public class HuntStartEvent extends Event {

    public static final String EVENT_TYPE = "HuntStart";

    private final String runId;

    public HuntStartEvent(String runId) {
        super(EVENT_TYPE);
        this.runId = checkNotNull(runId);
    }

    public String getRunId() {
        return runId;
    }
}
