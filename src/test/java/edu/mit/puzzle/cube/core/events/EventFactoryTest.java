package edu.mit.puzzle.cube.core.events;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventFactoryTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testDeserializationWithoutRequiredAttributes() throws IOException {
        String json = "{\"eventType\":\"HuntStart\"}";

        exception.expect(IllegalArgumentException.class);
        Event event = new EventFactory().generate(json);
    }

    @Test
    public void testDeserializationWithAttributes() throws IOException {
        String json = "{\"eventType\":\"HuntStart\",\"runId\":\"testHunt\"}";
        Event event = new EventFactory().generate(json);
        assertTrue(HuntStartEvent.class.isInstance(event));
        assertEquals("testHunt", ((HuntStartEvent) event).getRunId());
    }

    @Test
    public void testDeserializationUnexpectedEventType() throws IOException {
        String json = "{\"eventType\":\"BogusType\"}";

        exception.expect(IllegalArgumentException.class);
        Event event = new EventFactory().generate(json);
    }

}
