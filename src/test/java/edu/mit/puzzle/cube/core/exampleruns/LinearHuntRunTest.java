package edu.mit.puzzle.cube.core.exampleruns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.DevelopmentEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.serverresources.AbstractCubeResource;
import edu.mit.puzzle.cube.core.serverresources.EventsResource;
import edu.mit.puzzle.cube.core.serverresources.SubmissionResource;
import edu.mit.puzzle.cube.core.serverresources.SubmissionsResource;
import edu.mit.puzzle.cube.core.serverresources.TeamResource;
import edu.mit.puzzle.cube.core.serverresources.VisibilitiesResource;
import edu.mit.puzzle.cube.core.serverresources.VisibilityResource;
import edu.mit.puzzle.cube.huntimpl.linearexample.LinearExampleHuntDefinition;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.routing.Router;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LinearHuntRunTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private Router router;

    @Before
    public void setup() throws Exception {
        HuntDefinition huntDefinition = new LinearExampleHuntDefinition();
        ServiceEnvironment serviceEnvironment = new DevelopmentEnvironment(huntDefinition);

        ConnectionFactory connectionFactory = serviceEnvironment.getConnectionFactory();

        CompositeEventProcessor eventProcessor = new CompositeEventProcessor();
        SubmissionStore submissionStore = new SubmissionStore(
                connectionFactory,
                eventProcessor
        );
        HuntStatusStore huntStatusStore = new HuntStatusStore(
                connectionFactory,
                huntDefinition.getVisibilityStatusSet(),
                eventProcessor
        );

        huntDefinition.addToEventProcessor(eventProcessor, huntStatusStore);

        Context context = mock(Context.class, Mockito.RETURNS_SMART_NULLS);
        when(context.getAttributes()).thenReturn(new ConcurrentHashMap<>(
                ImmutableMap.of(
                        AbstractCubeResource.SUBMISSION_STORE_KEY, submissionStore,
                        AbstractCubeResource.HUNT_STATUS_STORE_KEY, huntStatusStore,
                        AbstractCubeResource.EVENT_PROCESSOR_KEY, eventProcessor
                )));
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        when(context.getExecutorService()).thenReturn(executorService);
        Logger logger = mock(Logger.class, Mockito.RETURNS_SMART_NULLS);
        when(context.getLogger()).thenReturn(logger);

        router = new Router(context);
        router.attach("/submissions", SubmissionsResource.class);
        router.attach("/submissions/{id}", SubmissionResource.class);
        router.attach("/visibilities", VisibilitiesResource.class);
        router.attach("/visibilities/{teamId}/{puzzleId}", VisibilityResource.class);
        router.attach("/events", EventsResource.class);
        router.attach("/teams/{id}", TeamResource.class);

        SimpleAccountRealm realm = new SimpleAccountRealm();
        realm.addRole("admin");
        realm.addAccount("adminuser", "adminpassword", "admin");
        realm.setRolePermissionResolver((String role) -> {
            switch (role) {
            case "admin":
                return ImmutableList.of(new WildcardPermission("*"));
            }
            return ImmutableList.<Permission>of();
        });
        SecurityManager securityManager = new DefaultSecurityManager(realm);
        SecurityUtils.setSecurityManager(securityManager);
        Subject subject = SecurityUtils.getSubject();
        subject.login(new UsernamePasswordToken("adminuser", "adminpassword"));
    }

    private JsonNode getAllSubmissions() throws IOException {
        Request request = new Request(Method.GET, "/submissions");
        Response response = router.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("submissions"));
        assertTrue(responseJson.get("submissions").isArray());
        return responseJson;
    }

    private JsonNode getVisibility(String teamId, String puzzleId) throws IOException {
        Request request = new Request(Method.GET, String.format("/visibilities/%s/%s",teamId,puzzleId));
        Response response = router.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("status"));
        assertTrue(responseJson.get("status").isTextual());
        return responseJson;
    }

    private JsonNode postHuntStart() throws IOException {
        String json = "{\"eventType\":\"HuntStart\"}";
        Representation representation = new JsonRepresentation(json);
        Request request = new Request(Method.POST, "/events", representation);
        Response response = router.handle(request);
        assertEquals(200, response.getStatus().getCode());
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("processed"));
        assertTrue(responseJson.get("processed").asBoolean());
        return responseJson;
    }

    private JsonNode postFullRelease(String puzzleId) throws IOException {
        String json = String.format("{\"eventType\":\"FullRelease\",\"puzzleId\":\"%s\"}", puzzleId);
        Representation representation = new JsonRepresentation(json);
        Request request = new Request(Method.POST, "/events", representation);
        Response response = router.handle(request);
        assertEquals(200, response.getStatus().getCode());
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("processed"));
        assertTrue(responseJson.get("processed").asBoolean());
        return responseJson;
    }

    private JsonNode postNewSubmission(String teamId, String puzzleId, String submission) throws IOException {
        String json = String.format("{\"teamId\":\"%s\",\"puzzleId\":\"%s\",\"submission\":\"%s\"}",
                teamId, puzzleId, submission);
        Representation representation = new JsonRepresentation(json);
        Request request = new Request(Method.POST, "/submissions", representation);
        Response response = router.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("created"));
        assertEquals(true, responseJson.get("created").asBoolean());
        return responseJson;
    }

    private JsonNode getSubmission(Integer submissionId) throws IOException {
        Request request = new Request(Method.GET, String.format("/submissions/%d",submissionId));
        Response response = router.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("status"));
        assertTrue(responseJson.get("status").isTextual());
        return responseJson;
    }

    private JsonNode postUpdateSubmission(Integer submissionId, String status) throws IOException {
        String json = String.format("{\"status\":\"%s\"}", status);
        Representation representation = new JsonRepresentation(json);
        Request request = new Request(Method.POST,
                String.format("/submissions/%d", submissionId),
                representation);
        Response response = router.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("updated"));
        assertEquals(true, responseJson.get("updated").asBoolean());
        return responseJson;
    }

    @Test
    public void testSubmittingAndUnlock() throws IOException {
        JsonNode json = getAllSubmissions();
        assertEquals(0, json.get("submissions").size());

        json = getVisibility("testerteam","puzzle1");
        assertEquals("INVISIBLE", json.get("status").asText());
        json = getVisibility("testerteam","puzzle2");
        assertEquals("INVISIBLE", json.get("status").asText());

        postHuntStart();

        json = getVisibility("testerteam","puzzle1");
        assertEquals("UNLOCKED", json.get("status").asText());
        json = getVisibility("testerteam","puzzle2");
        assertEquals("INVISIBLE", json.get("status").asText());

        postNewSubmission("testerteam", "puzzle1", "guess");

        json = getSubmission(1);
        assertEquals("SUBMITTED", json.get("status").asText());
        json = getAllSubmissions();
        assertEquals(1, json.get("submissions").size());

        postUpdateSubmission(1, "CORRECT");

        json = getVisibility("testerteam", "puzzle1");
        assertEquals("SOLVED", json.get("status").asText());
        json = getVisibility("testerteam", "puzzle2");
        assertEquals("UNLOCKED", json.get("status").asText());

        json = getVisibility("testerteam", "puzzle5");
        assertEquals("INVISIBLE", json.get("status").asText());

        postFullRelease("puzzle5");
        json = getVisibility("testerteam", "puzzle5");
        assertEquals("UNLOCKED", json.get("status").asText());
    }

}
