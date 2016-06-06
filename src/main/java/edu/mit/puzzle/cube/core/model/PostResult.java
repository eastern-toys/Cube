package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class PostResult {
    @AutoValue.Builder
    public static abstract class Builder {
        @Nullable @JsonProperty("created") public abstract Builder setCreated(Boolean created);
        @Nullable @JsonProperty("processed") public abstract Builder setProcessed(Boolean processed);
        @Nullable @JsonProperty("updated") public abstract Builder setUpdated(Boolean updated);
        public abstract PostResult build();
    }

    public static Builder builder() {
        return new AutoValue_PostResult.Builder();
    }

    @Nullable
    @JsonProperty("created")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Boolean getCreated();

    @Nullable
    @JsonProperty("processed")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Boolean getProcessed();

    @Nullable
    @JsonProperty("updated")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Boolean getUpdated();
}
