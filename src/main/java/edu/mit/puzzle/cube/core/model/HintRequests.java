package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonDeserialize(builder = AutoValue_HintRequests.Builder.class)
public abstract class HintRequests {
	@AutoValue.Builder
	public static abstract class Builder {
		@JsonProperty("hintRequests") public abstract Builder setHintRequests(List<HintRequest> hintRequests);
		public abstract HintRequests build();
	}

	public static Builder builder() {
		return new AutoValue_HintRequests.Builder();
	}

	@JsonProperty("hintRequests") public abstract List<HintRequest> getHintRequests();
}
