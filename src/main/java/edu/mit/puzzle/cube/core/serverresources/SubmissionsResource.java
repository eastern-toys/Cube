package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.Submissions;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public class SubmissionsResource extends AbstractCubeResource {

    @Get
    public Submissions handleGet() {
        return Submissions.builder()
                .setSubmissions(submissionStore.getAllSubmissions())
                .build();
    }

    @Post
    public Representation handlePost(Submission submission) throws JsonProcessingException {
        String visibilityStatus = huntStatusStore.getVisibility(
                submission.getTeamId(),
                submission.getPuzzleId());
        if (!huntStatusStore.getVisibilityStatusSet().allowsSubmissions(visibilityStatus)) {
            return new JsonRepresentation(MAPPER.writeValueAsString(ImmutableMap.of("created", false)));
        }

        boolean success = submissionStore.addSubmission(submission);
        return new JsonRepresentation(MAPPER.writeValueAsString(ImmutableMap.of("created", success)));
    }
}
