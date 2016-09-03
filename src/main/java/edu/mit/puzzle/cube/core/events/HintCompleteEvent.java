package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import edu.mit.puzzle.cube.core.model.HintRequest;

@AutoValue
@JsonDeserialize(builder = AutoValue_HintCompleteEvent.Builder.class)
@JsonTypeName("HintComplete")
public abstract class HintCompleteEvent extends Event {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("hintRequest") public abstract Builder setHintRequest(HintRequest hintRequest);
        public abstract HintCompleteEvent build();
    }

    public static Builder builder() {
        return new AutoValue_HintCompleteEvent.Builder();
    }

    @JsonProperty("hintRequest") public abstract HintRequest getHintRequest();
}
