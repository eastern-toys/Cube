package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

import java.time.Instant;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_Submission.Builder.class)
public abstract class Submission {
    @AutoValue.Builder
    public static abstract class Builder {
        @Nullable @JsonProperty("submissionId") public abstract Builder setSubmissionId(Integer submissionId);
        @Nullable @JsonProperty("teamId") public abstract Builder setTeamId(String teamId);
        @Nullable @JsonProperty("puzzleId") public abstract Builder setPuzzleId(String puzzleId);
        @Nullable @JsonProperty("submission") public abstract Builder setSubmission(String submission);
        @Nullable @JsonProperty("status") public abstract Builder setStatus(SubmissionStatus status);

        @Nullable
        @JsonProperty("timestamp")
        @JsonDeserialize(using=InstantDeserializer.class)
        public abstract Builder setTimestamp(Instant timestamp);

        public abstract Submission build();
    }

    public static Builder builder() {
        return new AutoValue_Submission.Builder();
    }

    @Nullable @JsonProperty("submissionId") public abstract Integer getSubmissionId();
    @Nullable @JsonProperty("teamId") public abstract String getTeamId();
    @Nullable @JsonProperty("puzzleId") public abstract String getPuzzleId();
    @Nullable @JsonProperty("submission") public abstract String getSubmission();
    @Nullable @JsonProperty("status") public abstract SubmissionStatus getStatus();

    @Nullable
    @JsonProperty("timestamp")
    @JsonSerialize(using=InstantSerializer.class)
    public abstract Instant getTimestamp();
}
