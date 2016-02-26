package edu.mit.puzzle.cube.core.events;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class EventFactoryTest {

    @Test
    public void testDeserializationWithoutAttributes() throws IOException {
        String json = "{\"eventType\":\"HuntStart\"}";
        Event event = new EventFactory().generate(json);
        assertEquals("HuntStart", event.getEventType());
    }

    @Test
    public void testDeserializationWithAttributes() throws IOException {
        String json = "{\"eventType\":\"HuntStart\",\"runId\":\"test\"}";
        Event event = new EventFactory().generate(json);
        assertEquals("HuntStart", event.getEventType());
        assertEquals("test", event.getAttribute("runId"));
    }

}
