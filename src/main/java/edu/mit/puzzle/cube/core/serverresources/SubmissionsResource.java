package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.Submissions;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.SubmissionsPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import java.util.List;
import java.util.Optional;

public class SubmissionsResource extends AbstractCubeResource {

    private SubmissionStore.PaginationOptions getPaginationOptions() {
        SubmissionStore.PaginationOptions.Builder paginationOptions =
                SubmissionStore.PaginationOptions.builder();
        try {
            String value = getQueryValue("startSubmissionId");
            if (value != null) {
                paginationOptions.setStartSubmissionId(Integer.parseInt(value));
            }
            value = getQueryValue("pageSize");
            if (value != null) {
                paginationOptions.setPageSize(Integer.parseInt(value));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("query parameter is not an int");
        }
        return paginationOptions.build();
    }

    @Get
    public Submissions handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        List<Submission> submissions;
        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new SubmissionsPermission(teamId.get(), PermissionAction.READ));
            Optional<String> puzzleId = Optional.ofNullable(getQueryValue("puzzleId"));
            if (puzzleId.isPresent()) {
                submissions = submissionStore.getSubmissionsByTeamAndPuzzle(
                        getPaginationOptions(),
                        teamId.get(),
                        puzzleId.get()
                );
            } else {
                submissions = submissionStore.getSubmissionsByTeam(
                        getPaginationOptions(),
                        teamId.get()
                );
            }
        } else {
            SecurityUtils.getSubject().checkPermission(
                    new SubmissionsPermission("*", PermissionAction.READ));
            submissions = submissionStore.getAllSubmissions(getPaginationOptions());
        }
        return Submissions.builder().setSubmissions(submissions).build();
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
