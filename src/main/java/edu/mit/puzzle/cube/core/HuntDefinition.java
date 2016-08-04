package edu.mit.puzzle.cube.core;

import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.model.Answer;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface HuntDefinition {
    VisibilityStatusSet getVisibilityStatusSet();

    List<Answer> getPuzzleList();

    void addToEventProcessor(
            CompositeEventProcessor eventProcessor,
            HuntStatusStore huntStatusStore);

    static HuntDefinition forClassName(String className) {
        final Logger LOGGER = LoggerFactory.getLogger(HuntDefinition.class);
        HuntDefinition huntDefinition = null;
        try {
            @SuppressWarnings("unchecked")
            Class<HuntDefinition> huntDefinitionClass = (Class<HuntDefinition>) Class.forName(className);
            huntDefinition = huntDefinitionClass.newInstance();
            LOGGER.info("Using hunt definition {}", className);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Hunt definition class not found", e);
            System.exit(1);
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Failed to instantiate hunt definition", e);
            System.exit(1);
        }
        return huntDefinition;
    }
}
