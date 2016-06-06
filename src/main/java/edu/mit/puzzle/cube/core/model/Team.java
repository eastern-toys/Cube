package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

@AutoValue
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
        @Nullable @JsonProperty("teamProperties") public abstract Builder setTeamProperties(Map<String, Property> teamProperties);

        abstract Team autoBuild();

        public Team build() {
            Team team = autoBuild();
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
            return team;
        }
    }

    public static Builder builder() {
        return new AutoValue_Team.Builder();
    }

    @SuppressWarnings("unchecked")
    public <T extends Property> T getTeamProperty(Class<T> propertyClass) {
        Property property = getTeamProperties().get(propertyClass.getSimpleName());
        if (property != null) {
            return (T) property;
        }
        return null;
    }

    @JsonProperty("teamId") public abstract String getTeamId();
    @JsonProperty("teamProperties") public abstract Map<String, Property> getTeamProperties();
}
