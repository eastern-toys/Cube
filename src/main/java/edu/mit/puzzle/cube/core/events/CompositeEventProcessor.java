package edu.mit.puzzle.cube.core.events;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

public class CompositeEventProcessor implements EventProcessor<Event> {

    @SuppressWarnings("rawtypes")
    private Multimap<Class, EventProcessor> eventProcessors = HashMultimap.create();

    public CompositeEventProcessor() {

    }

    public <T extends Event> void addEventProcessor(
            Class<T> clazz,
            EventProcessor<T> eventProcessor
    ) {
        this.eventProcessors.put(clazz, eventProcessor);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void process(Event event) {
        //Loops over all entries in case we subclass Event types
        for (Class<? extends Event> eventKeyClass : eventProcessors.keys()) {
            if (eventKeyClass.isInstance(event)) {
                Collection<EventProcessor> eventTypeProcessors = eventProcessors.get(eventKeyClass);
                for (EventProcessor eventProcessor : eventTypeProcessors) {
                    eventProcessor.process(event);
                }
            }
        }
    }

}
