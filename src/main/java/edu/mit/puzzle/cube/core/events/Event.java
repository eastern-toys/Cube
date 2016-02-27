package edu.mit.puzzle.cube.core.events;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Event {

    private final String eventType;
    private final Map<String,Object> attributes;

    public Event(String eventType) {
        this(eventType, ImmutableMap.<String, Object>of());
    }

    public Event(String eventType, Map<String, Object> attributes) {
        this.eventType = checkNotNull(eventType);
        this.attributes = ImmutableMap.copyOf(attributes);
    }

    public String getEventType() {
        return eventType;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}
