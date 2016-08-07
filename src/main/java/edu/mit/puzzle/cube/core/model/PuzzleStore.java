package edu.mit.puzzle.cube.core.model;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A repository for all puzzle metadata, including the answers.
 */
public class PuzzleStore {
    private final Map<String, Puzzle> puzzles;

    public PuzzleStore(List<Puzzle> puzzleList) {
        puzzles = puzzleList.stream().collect(
                Collectors.toMap(Puzzle::getPuzzleId, Function.identity())
        );
    }

    public Puzzle getPuzzle(String puzzleId) {
        Puzzle puzzle = puzzles.get(puzzleId);
        if (puzzle == null) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_NOT_FOUND.getCode(),
                    String.format("Unknown puzzle id %s", puzzleId));
        }
        return puzzle;
    }
}
