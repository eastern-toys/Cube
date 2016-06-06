package edu.mit.puzzle.cube.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.service.StatusService;

public class CubeStatusService extends StatusService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @AutoValue
    public abstract static class JsonStatus {
        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder setCode(int code);
            public abstract Builder setDescription(String description);
            public abstract JsonStatus build();
        }

        public static Builder builder() {
            return new AutoValue_CubeStatusService_JsonStatus.Builder();
        }

        @JsonProperty("code") public abstract int getCode();
        @JsonProperty("description") public abstract String getDescription();
    }

    @Override
    public Status toStatus(Throwable throwable, Resource resource) {
        if (throwable instanceof ResourceException) {
            return toStatus(throwable.getCause());
        }
        return toStatus(throwable);
    }

    @Override
    public Status toStatus(Throwable throwable, Request request, Response response) {
        return toStatus(throwable);
    }

    private Status toStatus(Throwable throwable) {
        int code = 500;
        if (throwable instanceof AuthenticationException) {
            code = 401;
        } else if (throwable instanceof AuthorizationException) {
            code = 403;
        }
        return new Status(code, throwable, throwable.getMessage());
    }

    @Override
    public Representation toRepresentation(Status status, Request request, Response response) {
        try {
            return new JsonRepresentation(MAPPER.writeValueAsString(JsonStatus.builder()
                    .setCode(status.getCode())
                    .setDescription(status.getReasonPhrase())
                    .build()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}