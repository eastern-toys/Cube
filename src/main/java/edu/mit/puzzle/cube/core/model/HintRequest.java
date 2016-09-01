package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

import java.time.Instant;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_HintRequest.Builder.class)
public abstract class HintRequest {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("hintRequestId") public abstract Builder setHintRequestId(Integer hintRequestId);
        @JsonProperty("teamId") public abstract Builder setTeamId(String teamId);
        @JsonProperty("puzzleId") public abstract Builder setPuzzleId(String puzzleId);
        @JsonProperty("status") public abstract Builder setStatus(HintRequestStatus status);
        @JsonProperty("callerUsername") public abstract Builder setCallerUsername(String callerUsername);
        @JsonProperty("request") public abstract Builder setRequest(@Nullable String request);
        @JsonProperty("response") public abstract Builder setResponse(@Nullable String response);

        @JsonProperty("timestamp")
        @JsonDeserialize(using=InstantDeserializer.class)
        public abstract Builder setTimestamp(Instant timestamp);

        public abstract HintRequest build();
    }

    public static Builder builder() {
        return new AutoValue_HintRequest.Builder();
    }

    @Nullable @JsonProperty("hintRequestId") public abstract Integer getHintRequestId();
    @Nullable @JsonProperty("teamId") public abstract String getTeamId();
    @Nullable @JsonProperty("puzzleId") public abstract String getPuzzleId();
    @Nullable @JsonProperty("status") public abstract HintRequestStatus getStatus();
    @Nullable @JsonProperty("callerUsername") public abstract String getCallerUsername();
    @Nullable @JsonProperty("request") public abstract String getRequest();
    @Nullable @JsonProperty("response") public abstract String getResponse();

    @Nullable
    @JsonProperty("timestamp")
    @JsonSerialize(using=InstantSerializer.class)
    public abstract Instant getTimestamp();
}
