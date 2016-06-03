package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_FullReleaseEvent.Builder.class)
@JsonTypeName("FullRelease")
public abstract class FullReleaseEvent extends Event {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("puzzleId") public abstract Builder setPuzzleId(String puzzleId);
        public abstract FullReleaseEvent build();
    }

    public static Builder builder() {
        return new AutoValue_FullReleaseEvent.Builder();
    }

    @JsonProperty("puzzleId") public abstract String getPuzzleId();
}
