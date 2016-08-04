package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
@JsonDeserialize(builder = AutoValue_Answer.Builder.class)
public abstract class Answer {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("puzzleId")
        public abstract Builder setPuzzleId(String puzzleId);

        @JsonProperty("canonicalAnswers")
        public abstract Builder setCanonicalAnswers(List<String> canonicalAnswers);

        @JsonProperty("acceptableAnswers")
        public abstract Builder setAcceptableAnswers(List<String> acceptableAnswers);

        abstract Answer build();
    }

    public static Builder builder() {
        return new AutoValue_Answer.Builder();
    }

    public abstract Builder toBuilder();

    public static Answer create(String puzzleId, String answer) {
        return builder()
                .setPuzzleId(puzzleId)
                .setCanonicalAnswers(ImmutableList.of(answer))
                .setAcceptableAnswers(ImmutableList.of(answer))
                .build();
    }

    @JsonProperty("puzzleId")
    public abstract String getPuzzleId();

    @JsonProperty("canonicalAnswers")
    public abstract List<String> getCanonicalAnswers();

    @JsonProperty("acceptableAnswers")
    public abstract List<String> getAcceptableAnswers();
}
