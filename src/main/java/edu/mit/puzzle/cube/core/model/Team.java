package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_Team.Builder.class)
public abstract class Team {
    public static abstract class Property {
        private static Map<String, Class<? extends Property>> propertyClasses = new HashMap<>();

        protected static void registerClass(Class<? extends Property> propertyClass) {
            propertyClasses.put(propertyClass.getSimpleName(), propertyClass);
        }

        public static Class<? extends Property> getClass(String propertyClassName) {
            return propertyClasses.get(propertyClassName);
        }
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("teamId") public abstract Builder setTeamId(String teamId);

        @Nullable
        @JsonProperty("password") public abstract Builder setPassword(@Nullable String password);

        @Nullable
        @JsonProperty("teamProperties")
        public abstract Builder setTeamProperties(@Nullable Map<String, Property> teamProperties);

        abstract Team autoBuild();

        public Team build() {
            Team team = autoBuild();
            if (team.getTeamProperties() != null) {
                for (Map.Entry<String, Property> entry : team.getTeamProperties().entrySet()) {
                    Class<? extends Property> propertyClass = Property.getClass(entry.getKey());
                    Preconditions.checkNotNull(
                            propertyClass,
                            "Team property class %s is not registered",
                            entry.getKey());
                    Preconditions.checkState(
                            propertyClass.isInstance(entry.getValue()),
                            "Team property object %s has wrong type",
                            entry.getKey());
                }
            }
            return team;
        }
    }

    public static Builder builder() {
        return new AutoValue_Team.Builder();
    }

    public abstract Builder toBuilder();

    @SuppressWarnings("unchecked")
    public <T extends Property> T getTeamProperty(Class<T> propertyClass) {
        if (getTeamProperties() == null) {
            return null;
        }
        Property property = getTeamProperties().get(propertyClass.getSimpleName());
        if (property != null) {
            return (T) property;
        }
        return null;
    }

    @JsonProperty("teamId") public abstract String getTeamId();

    @Nullable
    @JsonProperty("password")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract String getPassword();

    @Nullable
    @JsonProperty("teamProperties")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Map<String, Property> getTeamProperties();
}
