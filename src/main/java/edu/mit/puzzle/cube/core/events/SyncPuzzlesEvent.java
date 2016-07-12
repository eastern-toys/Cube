package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_SyncPuzzlesEvent.Builder.class)
@JsonTypeName("SyncPuzzles")
public abstract class SyncPuzzlesEvent extends Event {
    @AutoValue.Builder
    public static abstract class Builder {
        public abstract SyncPuzzlesEvent build();
    }

    public static Builder builder() {
        return new AutoValue_SyncPuzzlesEvent.Builder();
    }
}
