
package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_User.Builder.class)
public abstract class User {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("username") public abstract Builder setUsername(String username);

        public abstract User build();
    }

    public static Builder builder() {
        return new AutoValue_User.Builder();
    }

    @JsonProperty("username") public abstract String getUsername();
}