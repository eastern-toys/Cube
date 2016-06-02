package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
@JsonDeserialize(builder = AutoValue_Visibilities.Builder.class)
public abstract class Visibilities {
	@AutoValue.Builder
	public static abstract class Builder {
		@JsonProperty("visibilities") public abstract Builder setVisibilities(List<Visibility> visibilities);
		public abstract Visibilities build();
	}
	
	public static Builder builder() {
		return new AutoValue_Visibilities.Builder();
	}
	
	@JsonProperty("visibilities") public abstract List<Visibility> getVisibilities();
}
