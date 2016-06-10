package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Submission;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

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
            return submission.get();
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Submission not found");
            return null;
        }
    }

    @Post
    public PostResult handlePost(Submission submission) {
        int id = getId();
        if (submission.getStatus() == null) {
            return PostResult.builder().setUpdated(false).build();
        }
        boolean changed = submissionStore.setSubmissionStatus(id, submission.getStatus());
        return PostResult.builder().setUpdated(changed).build();
    }
}
