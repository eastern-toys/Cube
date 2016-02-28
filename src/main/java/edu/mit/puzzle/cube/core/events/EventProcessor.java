package edu.mit.puzzle.cube.core.events;

public interface EventProcessor<T extends Event> {

    public void process(T event);

}
