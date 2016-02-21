package edu.mit.puzzle.cube.core.events;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class CompositeEventProcessor implements EventProcessor {

    private List<EventProcessor> eventProcessors;

    public CompositeEventProcessor() {

    }

    public void setEventProcessors(List<EventProcessor> eventProcessors) {
        this.eventProcessors = ImmutableList.copyOf(eventProcessors);
    }

    public void process(Event event) {
        for (EventProcessor eventProcessor : eventProcessors) {
            eventProcessor.process(event);
        }
    }

}
