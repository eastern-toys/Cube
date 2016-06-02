package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
@JsonDeserialize(builder = AutoValue_Submissions.Builder.class)
public abstract class Submissions {
	@AutoValue.Builder
	public static abstract class Builder {
		@JsonProperty("submissions") public abstract Builder setSubmissions(List<Submission> submissions);
		public abstract Submissions build();
	}
	
	public static Builder builder() {
		return new AutoValue_Submissions.Builder();
	}
	
	@JsonProperty("submissions") public abstract List<Submission> getSubmissions();
}
