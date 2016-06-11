package edu.mit.puzzle.cube.core.exampleruns;

import com.fasterxml.jackson.databind.JsonNode;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.RestletTest;
import edu.mit.puzzle.cube.huntimpl.linearexample.LinearExampleHuntDefinition;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class LinearHuntRunTest extends RestletTest {
    protected HuntDefinition createHuntDefinition() {
        return new LinearExampleHuntDefinition();
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
