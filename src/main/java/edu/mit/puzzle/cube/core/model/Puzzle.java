package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Model for something that's solvable by a team. This could be a normal puzzle, or a metapuzzle,
 * or a live event, etc.
 *
 * Usually a puzzle is solved by entering a single answer, so the answers property will usually
 * have a length of 1. It is possible that some puzzles may be partially solvable and require
 * multiple distinct answers to be entered, in which case the answers property will have a length
 * greater than 1. It is also possible that solving a puzzle is determined by an external
 * interaction, not by entering an answer, in which case the answers property may be empty.
 *
 * The answers property will be omitted when returning puzzle metadata to solving teams.
 */
@AutoValue
@JsonDeserialize(builder = AutoValue_Puzzle.Builder.class)
public abstract class Puzzle {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("puzzleId")
        public abstract Builder setPuzzleId(String puzzleId);

        @JsonProperty("displayName")
        public abstract Builder setDisplayName(String displayName);

        @JsonProperty("answers")
        public abstract Builder setAnswers(@Nullable List<Answer> answers);

        public abstract Puzzle build();
    }

    public static Builder builder() {
        return new AutoValue_Puzzle.Builder();
    }

    public abstract Builder toBuilder();

    public static Puzzle create(String puzzleId, String answer) {
        return builder()
                .setPuzzleId(puzzleId)
                .setDisplayName(puzzleId)
                .setAnswers(Answer.createSingle(answer))
                .build();
    }

    @JsonProperty("puzzleId")
    public abstract String getPuzzleId();

    @JsonProperty("displayName")
    public abstract String getDisplayName();

    @Nullable
    @JsonProperty("answers")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract List<Answer> getAnswers();
}
