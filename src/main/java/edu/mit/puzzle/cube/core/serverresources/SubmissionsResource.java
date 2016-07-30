package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.Submissions;
import edu.mit.puzzle.cube.core.model.SubmissionStore.FilterOptions;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.SubmissionsPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SubmissionsResource extends AbstractCubeResource {

    private SubmissionStore.FilterOptions getFilterOptions() {
        List<SubmissionStatus> statuses = ImmutableList.<SubmissionStatus>of();
        if (getQueryValue("status") != null) {
            statuses = ImmutableList.copyOf(Splitter.on(",").split(getQueryValue("status")))
                    .stream()
                    .map(statusString -> SubmissionStatus.valueOf(statusString))
                    .collect(Collectors.toList());
        }
        return FilterOptions.builder()
                .setTeamId(Optional.ofNullable(getQueryValue("teamId")))
                .setPuzzleId(Optional.ofNullable(getQueryValue("puzzleId")))
                .setStatuses(statuses)
                .setCallerUsername(Optional.ofNullable(getQueryValue("callerUsername")))
                .build();
    }

    private SubmissionStore.PaginationOptions getPaginationOptions() {
        return SubmissionStore.PaginationOptions.builder()
                .setStartSubmissionId(Optional.ofNullable(getQueryValue("startSubmissionId")).map(
                        p -> Integer.parseInt(p)))
                .setPageSize(Optional.ofNullable(getQueryValue("pageSize")).map(
                        p -> Integer.parseInt(p)))
                .build();
    }

    @Get
    public Submissions handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));

        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new SubmissionsPermission(teamId.get(), PermissionAction.READ));
        } else {
            SecurityUtils.getSubject().checkPermission(
                    new SubmissionsPermission("*", PermissionAction.READ));
        }

        List<Submission> submissions = submissionStore.getSubmissions(
                getFilterOptions(),
                getPaginationOptions()
        );
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
