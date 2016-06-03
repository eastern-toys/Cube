package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonDeserialize(builder = AutoValue_VisibilityChanges.Builder.class)
public abstract class VisibilityChanges {
	@AutoValue.Builder
	public static abstract class Builder {
		@JsonProperty("visibilityChanges") public abstract Builder setVisibilityChanges(List<VisibilityChange> visibilityChanges);
		public abstract VisibilityChanges build();
	}

	public static Builder builder() {
		return new AutoValue_VisibilityChanges.Builder();
	}

	@JsonProperty("visibilityChanges") public abstract List<VisibilityChange> getVisibilityChanges();
}
