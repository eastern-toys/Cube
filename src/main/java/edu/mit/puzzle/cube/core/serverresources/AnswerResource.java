package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.Answer;
import edu.mit.puzzle.cube.core.permissions.AnswersPermission;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.restlet.resource.Get;

public class AnswerResource extends AbstractCubeResource {

    private String getId() {
        String idString = (String) getRequest().getAttributes().get("id");
        if (idString == null) {
            throw new IllegalArgumentException("puzzle id must be specified");
        }
        return idString;
    }

    @Get
    public Answer handleGet() {
        String puzzleId = getId();

        // An answer may be revealed to a user when one of these conditions is true:
        //   * The user is a solving team that has solved the puzzle (the puzzle's visibility status
        //     is in the set of "answer revealed" statuses)
        //   * The user has the "answers:read" permission (e.g. is a member of the writing team)
        Subject subject = SecurityUtils.getSubject();
        String currentUsername = (String) subject.getPrincipal();
        if (!huntStatusStore.getVisibilityStatusSet().getAnswerRevealedStatuses().contains(
                huntStatusStore.getVisibility(currentUsername, puzzleId))) {
            subject.checkPermission(new AnswersPermission());
        }

        return answerStore.getAnswer(puzzleId);
    }
}
