package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_Visibility.Builder.class)
public abstract class Visibility {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("teamId") public abstract Builder setTeamId(String teamId);
        @JsonProperty("puzzleId") public abstract Builder setPuzzleId(String puzzleId);
        @JsonProperty("status") public abstract Builder setStatus(String status);
        public abstract Visibility build();
    }

    public static Builder builder() {
        return new AutoValue_Visibility.Builder();
    }

    @JsonProperty("teamId") public abstract String getTeamId();
    @JsonProperty("puzzleId") public abstract String getPuzzleId();
    @JsonProperty("status") public abstract String getStatus();
}
