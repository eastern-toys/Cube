package edu.mit.puzzle.cube.core;

import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;

import java.util.List;

public interface HuntDefinition {

    VisibilityStatusSet getVisibilityStatusSet();

    List<String> getPuzzleList();

    void addToEventProcessor(
            CompositeEventProcessor eventProcessor,
            HuntStatusStore huntStatusStore);
}
