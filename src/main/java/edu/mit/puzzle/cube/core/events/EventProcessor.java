package edu.mit.puzzle.cube.core.events;

import java.util.concurrent.CompletableFuture;

public interface EventProcessor {

    public void process(Event event);

}
