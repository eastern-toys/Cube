package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.Submissions;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.SubmissionsPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import java.util.Optional;

public class SubmissionsResource extends AbstractCubeResource {

    @Get
    public Submissions handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new SubmissionsPermission(teamId.get(), PermissionAction.READ));
            Optional<String> puzzleId = Optional.ofNullable(getQueryValue("puzzleId"));
            if (puzzleId.isPresent()) {
                return Submissions.builder()
                        .setSubmissions(submissionStore.getSubmissionsByTeamAndPuzzle(teamId.get(), puzzleId.get()))
                        .build();
            } else {
                return Submissions.builder()
                        .setSubmissions(submissionStore.getSubmissionsByTeam(teamId.get()))
                        .build();
            }
        } else {
            SecurityUtils.getSubject().checkPermission(
                    new SubmissionsPermission("*", PermissionAction.READ));
            return Submissions.builder()
                    .setSubmissions(submissionStore.getAllSubmissions())
                    .build();
        }
    }

    @Post
    public PostResult handlePost(Submission submission) {
        SecurityUtils.getSubject().checkPermission(
                new SubmissionsPermission(submission.getTeamId(), PermissionAction.CREATE));
        String visibilityStatus = huntStatusStore.getVisibility(
                submission.getTeamId(),
                submission.getPuzzleId());
        if (!huntStatusStore.getVisibilityStatusSet().allowsSubmissions(visibilityStatus)) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "This submission is not allowed due to puzzle visibility");
        }

        boolean success = submissionStore.addSubmission(submission);
        return PostResult.builder().setCreated(success).build();
    }
}
