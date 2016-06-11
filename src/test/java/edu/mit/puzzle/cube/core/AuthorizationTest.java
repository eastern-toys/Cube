package edu.mit.puzzle.cube.core;

import com.fasterxml.jackson.databind.JsonNode;

import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;

import org.apache.shiro.realm.Realm;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthorizationTest extends RestletTest {
    protected Realm createAuthenticationRealm() {
        CubeJdbcRealm realm = new CubeJdbcRealm();
        realm.setDataSource(connectionFactory.getDataSource());
        return realm;
    }

    @Test
    public void createAndAuthorizeUser_noRoles() {
        JsonNode json = post("/users", String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                TESTERTEAM_CREDENTIALS.getIdentifier(),
                new String(TESTERTEAM_CREDENTIALS.getSecret())
        ));
        assertTrue(json.has("created"));
        assertTrue(json.get("created").asBoolean());

        json = get(String.format("/users/%s", TESTERTEAM_CREDENTIALS.getIdentifier()));
        assertEquals(TESTERTEAM_CREDENTIALS.getIdentifier(), json.get("username").asText());

        currentUserCredentials = TESTERTEAM_CREDENTIALS;

        json = get(String.format(
                "/authorized?permission=userinfo:read:%s",
                TESTERTEAM_CREDENTIALS.getIdentifier()
        ));
        assertTrue(json.has("authorized"));
        assertTrue(json.get("authorized").asBoolean());

        json = get("/authorized?permission=userinfo:read:anotheruser");
        assertTrue(json.has("authorized"));
        assertFalse(json.get("authorized").asBoolean());

        json = get("/authorized?permission=events:create:HuntStart");
        assertTrue(json.has("authorized"));
        assertFalse(json.get("authorized").asBoolean());
    }

    @Test
    public void createAndAuthorizeUser_writingTeamRole() {
        final String username = "authtestuser";
        final String password = "authtestpassword";
        ChallengeResponse credentials = new ChallengeResponse(
                ChallengeScheme.HTTP_BASIC, username, password);

        JsonNode json = post("/users", String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"roles\":[\"writingteam\"]}",
                username,
                password
        ));
        assertTrue(json.has("created"));
        assertTrue(json.get("created").asBoolean());

        json = get(String.format("/users/%s", username));
        assertEquals(username, json.get("username").asText());

        currentUserCredentials = credentials;

        json = get("/authorized?permission=submissions:read:testerteam");
        assertTrue(json.has("authorized"));
        assertTrue(json.get("authorized").asBoolean());

        json = get("/authorized?permission=events:create:HuntStart");
        assertTrue(json.has("authorized"));
        assertFalse(json.get("authorized").asBoolean());
    }
}
