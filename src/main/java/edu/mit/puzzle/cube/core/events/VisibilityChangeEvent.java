package edu.mit.puzzle.cube.core.events;

import edu.mit.puzzle.cube.core.model.Visibility;

import static com.google.common.base.Preconditions.checkNotNull;

public class VisibilityChangeEvent extends Event {

    public static final String EVENT_TYPE = "VisibilityChange";

    private final Visibility visibility;

    public VisibilityChangeEvent(Visibility visibility) {
        super(EVENT_TYPE);
        this.visibility = visibility;
    }

    public Visibility getVisibility() {
        return visibility;
    }
}
