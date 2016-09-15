package edu.mit.puzzle.cube.core.events;

import java.util.List;

public interface EventProcessor<T extends Event> {

    public void process(T event);

    default void processBatch(List<? extends T> events) {
        events.forEach(this::process);
    }

}
