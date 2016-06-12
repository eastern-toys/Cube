package edu.mit.puzzle.cube.core.permissions;

import edu.mit.puzzle.cube.core.events.Event;

public class EventsPermission extends InstanceLevelPermission {
    private static final long serialVersionUID = 1L;

    public EventsPermission(Event event, PermissionAction... actions) {
        this(event.getClass(), actions);
    }

    public EventsPermission(Class<? extends Event> eventClass, PermissionAction... actions) {
        super("events", eventClass.getSimpleName(), actions);
    }
}
