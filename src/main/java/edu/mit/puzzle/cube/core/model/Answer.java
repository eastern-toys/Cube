package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Model for an answer string for a puzzle. There may be multiple acceptable forms or
 * spellings for this answer (which may be added to acceptableAnswers), but the single
 * canonicalAnswer string is the canonical acceptable answer that will be shown to the solving
 * team after they enter this answer correctly.
 */
@AutoValue
@JsonDeserialize(builder = AutoValue_Answer.Builder.class)
public abstract class Answer {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("canonicalAnswer")
        public abstract Builder setCanonicalAnswer(String canonicalAnswer);

        @JsonProperty("acceptableAnswers")
        public abstract Builder setAcceptableAnswers(List<String> acceptableAnswers);

        abstract Answer autoBuild();

        public Answer build() {
            Answer answer = autoBuild();
            Preconditions.checkState(
                    answer.getAcceptableAnswers().contains(answer.getCanonicalAnswer()),
                    "The canonical answer must also be an acceptable answer"
            );
            return answer;
        }
    }

    public static Builder builder() {
        return new AutoValue_Answer.Builder();
    }

    public abstract Builder toBuilder();

    public static Answer create(String answer) {
        return builder()
                .setCanonicalAnswer(answer)
                .setAcceptableAnswers(ImmutableList.of(answer))
                .build();
    }

    public static ImmutableList<Answer> createSingle(String answer) {
        return ImmutableList.of(Answer.create(answer));
    }

    @JsonProperty("canonicalAnswer")
    public abstract String getCanonicalAnswer();

    @JsonProperty("acceptableAnswers")
    public abstract List<String> getAcceptableAnswers();
}
