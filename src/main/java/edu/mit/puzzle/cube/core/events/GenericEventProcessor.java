package edu.mit.puzzle.cube.core.events;

public interface GenericEventProcessor extends EventProcessor<Event> {

    public void process(Event event);

}
