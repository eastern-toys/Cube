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
        map.remove("eventType");

        return new Event(eventType, map);
    }

}
