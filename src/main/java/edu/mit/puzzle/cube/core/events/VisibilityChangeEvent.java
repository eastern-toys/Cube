package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import edu.mit.puzzle.cube.core.model.Visibility;

@AutoValue
@JsonDeserialize(builder = AutoValue_VisibilityChangeEvent.Builder.class)
@JsonTypeName("VisibilityChange")
public abstract class VisibilityChangeEvent extends Event {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("visibility") public abstract Builder setVisibility(Visibility visibility);
        public abstract VisibilityChangeEvent build();
    }

    public static Builder builder() {
        return new AutoValue_VisibilityChangeEvent.Builder();
    }

    @JsonProperty("visibility") public abstract Visibility getVisibility();
}
