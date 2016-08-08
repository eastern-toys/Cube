package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.SubmissionsPermission;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import java.util.Optional;

public class SubmissionResource extends AbstractCubeResource {

    private int getId() {
        String idString = (String) getRequest().getAttributes().get("id");
        if (idString == null) {
            throw new IllegalArgumentException("id must be specified");
        }
        try {
            return Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id is not valid");
        }
    }

    @Get
    public Submission handleGet() {
        int id = getId();
        Optional<Submission> submission = submissionStore.getSubmission(id);

        if (submission.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new SubmissionsPermission(submission.get().getTeamId(), PermissionAction.READ));
            return submission.get();
        } else {
            throw new ResourceException(
                    Status.CLIENT_ERROR_NOT_FOUND,
                    String.format("Submission %d does not exist", id));
        }
    }

    @Post
    public PostResult handlePost(Submission submission) {
        int id = getId();
        if (submission.getStatus() == null) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "A status must be specified when updating a submission");
        }

        Optional<Submission> existingSubmission = submissionStore.getSubmission(id);
        if (!existingSubmission.isPresent()) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_NOT_FOUND,
                    String.format("Submission %d does not exist", id));
        }

        Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(
                new SubmissionsPermission(existingSubmission.get().getTeamId(), PermissionAction.UPDATE));

        if (existingSubmission.get().getStatus().isTerminal()) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "This submission has already been handled and may no longer be changed.");
        }

        String currentUsername = (String) subject.getPrincipal();

        if (existingSubmission.get().getStatus().isAssigned()
                && submission.getStatus().isAssigned()
                && !existingSubmission.get().getCallerUsername().equals(currentUsername)) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    String.format(
                            "This submission is already claimed by %s. It must be unassigned " +
                            "before you can change it.",
                            existingSubmission.get().getCallerUsername()));
        }

        String callerUsername = null;
        if (submission.getStatus().isAssigned()) {
            callerUsername = currentUsername;
        }

        String canonicalAnswer = null;
        if (submission.getStatus().equals(SubmissionStatus.CORRECT)) {
            if (submission.getCanonicalAnswer() != null) {
                canonicalAnswer = submission.getCanonicalAnswer();
            } else {
                Puzzle puzzle = puzzleStore.getPuzzle(existingSubmission.get().getPuzzleId());
                if (puzzle.getAnswers().size() == 1) {
                    canonicalAnswer = puzzle.getAnswers().get(0).getCanonicalAnswer();
                } else {
                    // TODO: consider requiring the caller to provide a canonical answer, and
                    // fail this request in this case.
                    canonicalAnswer = submission.getSubmission();
                }
            }
        }

        boolean changed = submissionStore.setSubmissionStatus(
                id, submission.getStatus(), callerUsername, canonicalAnswer);
        return PostResult.builder().setUpdated(changed).build();
    }
}
