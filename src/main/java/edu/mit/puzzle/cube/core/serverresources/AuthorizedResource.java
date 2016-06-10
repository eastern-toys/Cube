package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import org.apache.shiro.SecurityUtils;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

public class AuthorizedResource extends AbstractCubeResource {

    @AutoValue
    public static abstract class Authorized {
        public static Authorized create(boolean authorized) {
            return new AutoValue_AuthorizedResource_Authorized(authorized);
        }
        @JsonProperty("authorized") public abstract boolean authorized();
    }

    @Get
    public Authorized handleGet() {
        String permission = getQueryValue("permission");
        if (permission == null) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "A request to /authorized must specify a permission to check");
        }
        return Authorized.create(SecurityUtils.getSubject().isPermitted(permission));
    }
}
