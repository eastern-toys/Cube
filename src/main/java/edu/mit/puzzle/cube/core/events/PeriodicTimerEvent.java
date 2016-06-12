package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_PeriodicTimerEvent.Builder.class)
@JsonTypeName("PeriodicTimer")
public abstract class PeriodicTimerEvent extends Event {
    @AutoValue.Builder
    public static abstract class Builder {
        public abstract PeriodicTimerEvent build();
    }

    public static Builder builder() {
        return new AutoValue_PeriodicTimerEvent.Builder();
    }
}
