package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_HuntStartEvent.Builder.class)
@JsonTypeName("HuntStart")
public abstract class HuntStartEvent extends Event {
    @AutoValue.Builder
    public static abstract class Builder {
        public abstract HuntStartEvent build();
    }

    public static Builder builder() {
        return new AutoValue_HuntStartEvent.Builder();
    }
}
