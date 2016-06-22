package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

import java.time.Instant;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_VisibilityChange.Builder.class)
public abstract class VisibilityChange {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("teamId") public abstract Builder setTeamId(String teamId);
        @JsonProperty("puzzleId") public abstract Builder setPuzzleId(String puzzleId);
        @JsonProperty("status") public abstract Builder setStatus(String status);

        @JsonProperty("timestamp")
        @JsonDeserialize(using=InstantDeserializer.class)
        public abstract Builder setTimestamp(Instant timestamp);

        @Nullable
        @JsonProperty("visibilityHistoryId")
        public abstract Builder setVisibilityHistoryId(Integer visibilityHistoryId);

        public abstract VisibilityChange build();
    }

    public static Builder builder() {
        return new AutoValue_VisibilityChange.Builder();
    }

    @JsonProperty("teamId") public abstract String getTeamId();
    @JsonProperty("puzzleId") public abstract String getPuzzleId();
    @JsonProperty("status") public abstract String getStatus();

    @JsonProperty("timestamp")
    @JsonSerialize(using=InstantSerializer.class)
    public abstract Instant getTimestamp();

    @Nullable
    @JsonProperty("visibilityHistoryId")
    public abstract Integer getVisibilityHistoryId();
}
