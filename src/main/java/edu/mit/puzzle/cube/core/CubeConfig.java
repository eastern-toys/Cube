package edu.mit.puzzle.cube.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_CubeConfig.Builder.class)
public abstract class CubeConfig {
    public enum ServiceEnvironment {
        DEVELOPMENT,
        PRODUCTION,
    }

    @AutoValue
    public static abstract class DatabaseConfig {
        @JsonCreator
        public static DatabaseConfig create(
                @JsonProperty("driverClassName") String driverClassName,
                @JsonProperty("jdbcUrl") String jdbcUrl,
                @JsonProperty("username") String username,
                @JsonProperty("password") String password
        ) {
            return new AutoValue_CubeConfig_DatabaseConfig(
                    driverClassName,
                    jdbcUrl,
                    username,
                    password
            );
        }

        @JsonProperty("driverClassName") public abstract String getDriverClassName();
        @JsonProperty("jdbcUrl") public abstract String getJdbcUrl();
        @JsonProperty("username") public abstract String getUsername();
        @JsonProperty("password") public abstract String getPassword();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("port") public abstract Builder setPort(int port);
        @JsonProperty("corsAllowedOrigins") public abstract Builder setCorsAllowedOrigins(Set<String> corsAllowedOrigins);
        @JsonProperty("huntDefinitionClassName") public abstract Builder setHuntDefinitionClassName(String huntDefinitionClassName);
        @JsonProperty("serviceEnvironment") public abstract Builder setServiceEnvironment(ServiceEnvironment serviceEnvironment);
        @Nullable @JsonProperty("databaseConfig") public abstract Builder setDatabaseConfig(DatabaseConfig databaseConfig);

        public abstract CubeConfig build();
    }

    public static Builder builder() {
        return new AutoValue_CubeConfig.Builder()
                .setPort(8182)
                .setCorsAllowedOrigins(ImmutableSet.of("http://localhost:8081"))
                .setHuntDefinitionClassName("edu.mit.puzzle.cube.huntimpl.linearexample.LinearExampleHuntDefinition")
                .setServiceEnvironment(ServiceEnvironment.DEVELOPMENT);
    }

    @JsonProperty("port") public abstract int getPort();
    @JsonProperty("corsAllowedOrigins") public abstract Set<String> getCorsAllowedOrigins();
    @JsonProperty("huntDefinitionClassName") public abstract String getHuntDefinitionClassName();
    @JsonProperty("serviceEnvironment") public abstract ServiceEnvironment getServiceEnvironment();
    @Nullable @JsonProperty("databaseConfig") public abstract DatabaseConfig getDatabaseConfig();
}
