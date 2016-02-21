package edu.mit.puzzle.cube.core.events;

import java.io.IOException;
import java.util.Optional;

public interface EventFactory {

    public Optional<Event> generateEvent(String json) throws IOException;

}
