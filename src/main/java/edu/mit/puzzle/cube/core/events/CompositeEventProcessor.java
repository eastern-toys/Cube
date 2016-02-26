package edu.mit.puzzle.cube.core.events;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

public class CompositeEventProcessor implements EventProcessor {

    private List<EventProcessor> eventProcessors = Lists.newArrayList();

    public CompositeEventProcessor() {

    }

    public void addEventProcessor(EventProcessor eventProcessor) {
        this.eventProcessors.add(eventProcessor);
    }

    public void addEventProcessors(List<EventProcessor> eventProcessors) {
        this.eventProcessors.addAll(eventProcessors);
    }

    public void process(Event event) {
        for (EventProcessor eventProcessor : eventProcessors) {
            eventProcessor.process(event);
        }
    }

}
