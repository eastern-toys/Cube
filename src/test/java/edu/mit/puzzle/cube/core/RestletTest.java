package edu.mit.puzzle.cube.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.DevelopmentEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.model.*;
import edu.mit.puzzle.cube.core.permissions.CubeRole;
import edu.mit.puzzle.cube.core.serverresources.AbstractCubeResource;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.LifecycleUtils;
import org.apache.shiro.util.ThreadState;
import org.junit.After;
import org.junit.Before;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Base class for a test that exercises REST routes.
 */
public abstract class RestletTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final ChallengeResponse ADMIN_CREDENTIALS =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "adminuser", "adminpassword");

    private static ThreadState subjectThreadState;

    protected Context context;
    protected Restlet restlet;
    protected ServiceEnvironment serviceEnvironment;

    protected ChallengeResponse currentUserCredentials;

    @Before
    public void setUp() throws SQLException {
        context = new Context();
        restlet = new CubeRestlet(context);

        HuntDefinition huntDefinition = createHuntDefinition();
        serviceEnvironment = new DevelopmentEnvironment(huntDefinition);
        ConnectionFactory connectionFactory = serviceEnvironment.getConnectionFactory();

        CompositeEventProcessor eventProcessor = huntDefinition.generateCompositeEventProcessor();
        HuntStatusStore huntStatusStore = new HuntStatusStore(
                connectionFactory,
                huntDefinition.getVisibilityStatusSet(),
                eventProcessor
        );
        SubmissionStore submissionStore = new SubmissionStore(
                connectionFactory,
                eventProcessor
        );
        UserStore userStore = new UserStore(
                connectionFactory
        );
        PuzzleStore puzzleStore = new PuzzleStore(
                huntDefinition.getPuzzles()
        );
        HintRequestStore hintRequestStore = new HintRequestStore(
                connectionFactory,
                huntDefinition,
                huntStatusStore,
                eventProcessor
        );

        huntDefinition.addToEventProcessor(eventProcessor, huntStatusStore);

        context.getAttributes().put(AbstractCubeResource.PUZZLE_STORE_KEY, puzzleStore);
        context.getAttributes().put(AbstractCubeResource.EVENT_PROCESSOR_KEY, eventProcessor);
        context.getAttributes().put(AbstractCubeResource.HINT_REQUEST_STORE_KEY, hintRequestStore);
        context.getAttributes().put(AbstractCubeResource.HUNT_STATUS_STORE_KEY, huntStatusStore);
        context.getAttributes().put(AbstractCubeResource.SUBMISSION_STORE_KEY, submissionStore);
        context.getAttributes().put(AbstractCubeResource.USER_STORE_KEY, userStore);

        Realm realm = createAuthenticationRealm();
        SecurityManager securityManager = new DefaultSecurityManager(realm);
        SecurityUtils.setSecurityManager(securityManager);

        subjectThreadState = new SubjectThreadState(SecurityUtils.getSubject());
        subjectThreadState.bind();

        currentUserCredentials = ADMIN_CREDENTIALS;
    }

    @After
    public void tearDown() {
        if (subjectThreadState != null) {
            subjectThreadState.clear();
            subjectThreadState = null;
        }
        try {
            SecurityManager securityManager = SecurityUtils.getSecurityManager();
            LifecycleUtils.destroy(securityManager);
        } catch (UnavailableSecurityManagerException e) {
        }
        SecurityUtils.setSecurityManager(null);
    }

    protected HuntDefinition createHuntDefinition() {
        return new HuntDefinition() {
            @Override
            public VisibilityStatusSet getVisibilityStatusSet() {
                return new StandardVisibilityStatusSet();
            }

            @Override
            public List<Puzzle> getPuzzles() {
                return ImmutableList.<Puzzle>of();
            }

            @Override
            public void addToEventProcessor(
                    CompositeEventProcessor eventProcessor,
                    HuntStatusStore huntStatusStore
            ) {
            }
        };
    }

    protected Realm createAuthenticationRealm() {
        SimpleAccountRealm realm = new SimpleAccountRealm();

        realm.addRole(CubeRole.ADMIN.getName());
        realm.addAccount(
                ADMIN_CREDENTIALS.getIdentifier(),
                new String(ADMIN_CREDENTIALS.getSecret()),
                CubeRole.ADMIN.getName()
        );

        realm.setRolePermissionResolver((String role) -> {
            switch (role) {
            case "admin":
                return ImmutableList.copyOf(CubeRole.ADMIN.getPermissions());
            }
            return ImmutableList.<Permission>of();
        });

        return realm;
    }

    protected void addUser(ChallengeResponse userCredentials, List<String> roles) {
        JsonNode json = post("/users", User.builder()
                .setUsername(userCredentials.getIdentifier())
                .setPassword(new String(userCredentials.getSecret()))
                .setRoles(roles)
                .build());
        assertTrue(json.has("created"));
        assertTrue(json.get("created").asBoolean());
    }

    protected void addTeam(ChallengeResponse teamCredentials) {
        JsonNode json = post("/teams", Team.builder()
                .setTeamId(teamCredentials.getIdentifier())
                .setPassword(new String(teamCredentials.getSecret()))
                .build());
        assertTrue(json.has("created"));
        assertTrue(json.get("created").asBoolean());
    }

    protected void setCurrentUserCredentials(ChallengeResponse userCredentials) {
        currentUserCredentials = userCredentials;
    }

    protected JsonNode get(String url) {
        Request request = new Request(Method.GET, url);
        request.setChallengeResponse(currentUserCredentials);
        Response response = restlet.handle(request);
        assertEquals(Status.SUCCESS_OK.getCode(), response.getStatus().getCode());
        return convertResponseToJson(response);
    }

    protected Status getExpectFailure(String url) {
        Request request = new Request(Method.GET, url);
        request.setChallengeResponse(currentUserCredentials);
        Response response = restlet.handle(request);
        assertNotEquals(Status.SUCCESS_OK.getCode(), response.getStatus().getCode());
        return response.getStatus();
    }

    protected JsonNode post(String url, Object body) {
        try {
            return post(url, MAPPER.writer().writeValueAsString(body));
        } catch (JsonProcessingException e) {
            fail("Failed to convert Java object to JSON for POST");
            throw new RuntimeException(e);
        }
    }

    protected JsonNode post(String url, String body) {
        Representation representation = new JsonRepresentation(body);
        Request request = new Request(Method.POST, url, representation);
        request.setChallengeResponse(currentUserCredentials);
        Response response = restlet.handle(request);
        assertEquals(Status.SUCCESS_OK.getCode(), response.getStatus().getCode());
        return convertResponseToJson(response);
    }

    protected Status postExpectFailure(String url, Object body) {
        try {
            return postExpectFailure(url, MAPPER.writer().writeValueAsString(body));
        } catch (JsonProcessingException e) {
            fail("Failed to convert Java object to JSON for POST");
            throw new RuntimeException(e);
        }
    }

    protected Status postExpectFailure(String url, String body) {
        Representation representation = new JsonRepresentation(body);
        Request request = new Request(Method.POST, url, representation);
        request.setChallengeResponse(currentUserCredentials);
        Response response = restlet.handle(request);
        assertNotEquals(Status.SUCCESS_OK.getCode(), response.getStatus().getCode());
        return response.getStatus();
    }

    protected JsonNode convertResponseToJson(Response response) {
        try {
            return MAPPER.readTree(response.getEntityAsText());
        } catch (IOException e) {
            fail("Failed to parse GET response as JSON: " + response.getEntityAsText());
            throw new RuntimeException(e);
        }
    }

    protected JsonNode getAllSubmissions() {
        JsonNode responseJson = get("/submissions");
        assertTrue(responseJson.has("submissions"));
        assertTrue(responseJson.get("submissions").isArray());
        return responseJson;
    }

    protected JsonNode getVisibility(String teamId, String puzzleId) {
        JsonNode responseJson = get(String.format("/visibilities/%s/%s", teamId, puzzleId));
        assertTrue(responseJson.has("status"));
        assertTrue(responseJson.get("status").isTextual());
        return responseJson;
    }

    protected JsonNode postVisibility(String teamId, String puzzleId, String status) {
        JsonNode responseJson = post(
                String.format("/visibilities/%s/%s", teamId, puzzleId),
                Visibility.builder()
                   .setTeamId(teamId)
                   .setPuzzleId(puzzleId)
                   .setStatus(status)
                   .build());
        assertTrue(responseJson.has("updated"));
        assertTrue(responseJson.get("updated").asBoolean());
        return responseJson;
    }

    protected JsonNode postHuntStart() {
        JsonNode responseJson = post("/events", "{\"eventType\":\"HuntStart\"}");
        assertTrue(responseJson.has("processed"));
        assertTrue(responseJson.get("processed").asBoolean());
        return responseJson;
    }

    protected JsonNode postFullRelease(String puzzleId) {
        JsonNode responseJson = post(
                "/events",
                String.format("{\"eventType\":\"FullRelease\",\"puzzleId\":\"%s\"}", puzzleId));
        assertTrue(responseJson.has("processed"));
        assertTrue(responseJson.get("processed").asBoolean());
        return responseJson;
    }

    protected JsonNode postNewSubmission(String teamId, String puzzleId, String submission) {
        JsonNode responseJson = post(
                "/submissions",
                String.format(
                        "{\"teamId\":\"%s\",\"puzzleId\":\"%s\",\"submission\":\"%s\"}",
                        teamId, puzzleId, submission
                )
        );
        assertTrue(responseJson.has("created"));
        assertTrue(responseJson.get("created").asBoolean());
        return responseJson;
    }

    protected JsonNode getSubmission(Integer submissionId) {
        JsonNode responseJson = get(String.format("/submissions/%d", submissionId));
        assertTrue(responseJson.has("status"));
        assertTrue(responseJson.get("status").isTextual());
        return responseJson;
    }

    protected JsonNode postUpdateSubmission(Integer submissionId, String status) {
        JsonNode responseJson = post(
                String.format("/submissions/%d", submissionId),
                String.format("{\"status\":\"%s\"}", status)
        );
        assertTrue(responseJson.has("updated"));
        assertTrue(responseJson.get("updated").asBoolean());
        return responseJson;
    }

}
