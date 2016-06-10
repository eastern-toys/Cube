package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
@JsonDeserialize(builder = AutoValue_Teams.Builder.class)
public abstract class Teams {
	@AutoValue.Builder
	public static abstract class Builder {
		@JsonProperty("teams") public abstract Builder setTeams(List<Team> teams);
		public abstract Teams build();
	}

	public static Builder builder() {
		return new AutoValue_Teams.Builder();
	}

	@JsonProperty("teams") public abstract List<Team> getTeams();
}
