package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Map;

public class EventFactory {

    protected static ObjectMapper MAPPER = new ObjectMapper();

    public Event generate(String json) throws IOException {
        Map<String,Object> map = Maps.newHashMap(MAPPER.readValue(json, Map.class));
        String eventType = (String) map.get("eventType");

        try {
            switch (eventType) {
                case FullReleaseEvent.EVENT_TYPE:
                    return new FullReleaseEvent((String) map.get("runId"), (String) map.get("puzzleId"));
                case HuntStartEvent.EVENT_TYPE:
                    return new HuntStartEvent((String) map.get("runId"));
                default:
                    throw new IllegalArgumentException("Unsupported event type");
            }
        } catch (NullPointerException | ClassCastException e) {
            throw new IllegalArgumentException("Event does not contain all required fields in correct formats");
        }
    }

}
