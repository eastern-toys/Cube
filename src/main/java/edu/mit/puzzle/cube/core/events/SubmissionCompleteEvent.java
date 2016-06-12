package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import edu.mit.puzzle.cube.core.model.Submission;

@AutoValue
@JsonDeserialize(builder = AutoValue_SubmissionCompleteEvent.Builder.class)
@JsonTypeName("SubmissionComplete")
public abstract class SubmissionCompleteEvent extends Event {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("submission") public abstract Builder setSubmission(Submission submission);
        public abstract SubmissionCompleteEvent build();
    }

    public static Builder builder() {
        return new AutoValue_SubmissionCompleteEvent.Builder();
    }

    @JsonProperty("submission") public abstract Submission getSubmission();
}
