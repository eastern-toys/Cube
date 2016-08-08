package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_Visibility.Builder.class)
public abstract class Visibility {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("teamId") public abstract Builder setTeamId(String teamId);
        @JsonProperty("puzzleId") public abstract Builder setPuzzleId(String puzzleId);
        @JsonProperty("status") public abstract Builder setStatus(String status);
        @JsonProperty("solvedAnswers") public abstract Builder setSolvedAnswers(List<String> solvedAnswers);

        @Nullable abstract List<String> getSolvedAnswers();

        abstract Visibility autoBuild();

        public Visibility build() {
            if (getSolvedAnswers() == null) {
                setSolvedAnswers(ImmutableList.of());
            }
            return autoBuild();
        }
    }

    public static Builder builder() {
        return new AutoValue_Visibility.Builder();
    }

    public abstract Builder toBuilder();

    @JsonProperty("teamId") public abstract String getTeamId();
    @JsonProperty("puzzleId") public abstract String getPuzzleId();
    @JsonProperty("status") public abstract String getStatus();
    @Nullable @JsonProperty("solvedAnswers") public abstract List<String> getSolvedAnswers();
}
