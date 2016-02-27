package edu.mit.puzzle.cube.core.events;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class EventFactoryTest {

    @Test
    public void testDeserializationWithoutAttributes() throws IOException {
        String json = "{\"eventType\":\"Generic\"}";
        Event event = new EventFactory().generate(json);
        assertEquals("Generic", event.getEventType());
    }

    @Test
    public void testDeserializationWithAttributes() throws IOException {
        String json = "{\"eventType\":\"Generic\",\"key\":\"value\"}";
        Event event = new EventFactory().generate(json);
        assertEquals("Generic", event.getEventType());
        assertEquals("value", event.getAttribute("key"));
    }

}
