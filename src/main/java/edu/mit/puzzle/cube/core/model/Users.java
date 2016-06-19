package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
@JsonDeserialize(builder = AutoValue_Users.Builder.class)
public abstract class Users {
	@AutoValue.Builder
	public static abstract class Builder {
		@JsonProperty("users") public abstract Builder setUsers(List<User> users);
		public abstract Users build();
	}

	public static Builder builder() {
		return new AutoValue_Users.Builder();
	}

	@JsonProperty("users") public abstract List<User> getUsers();
}
