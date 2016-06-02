package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.model.Submission;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
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
    public Submission handleGet() throws JsonProcessingException {
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
    public Representation handlePost(Submission submission) throws JsonProcessingException {
        int id = getId();
        if (submission.getStatus() == null) {
            return new JsonRepresentation(MAPPER.writeValueAsString(ImmutableMap.of("updated",false)));
        }
        boolean changed = submissionStore.setSubmissionStatus(id, submission.getStatus());
        return new JsonRepresentation(MAPPER.writeValueAsString(ImmutableMap.of("updated",changed)));
    }
}
