package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.core.permissions.AnswersPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;

public class PuzzleResource extends AbstractCubeResource {

    private String getId() {
        String idString = (String) getRequest().getAttributes().get("id");
        if (idString == null) {
            throw new IllegalArgumentException("puzzle id must be specified");
        }
        return idString;
    }

    @Get
    public Puzzle handleGet() {
        String puzzleId = getId();

        // TODO: when, if ever, should puzzle resources be exposed to solving teams, and what
        // portion of them should be exposed? For now, only writing team members with answers
        // permission can fetch these.
        SecurityUtils.getSubject().checkPermission(new AnswersPermission());

        Puzzle puzzle = puzzleStore.getPuzzle(puzzleId);

        return puzzle;
    }
}
