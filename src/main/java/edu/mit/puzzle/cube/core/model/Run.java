package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

import java.time.Instant;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_Run.Builder.class)
public abstract class Run {
    @AutoValue.Builder
    public static abstract class Builder {
        @Nullable
        @JsonProperty("startTimestamp")
        @JsonDeserialize(using=InstantDeserializer.class)
        public abstract Builder setStartTimestamp(Instant startTimestamp);

        abstract Run build();
    }

    public Builder builder() {
        return new AutoValue_Run.Builder();
    }

    @Nullable
    @JsonProperty("startTimestamp")
    @JsonSerialize(using=InstantSerializer.class)
    public abstract Instant getStartTimestamp();
}
