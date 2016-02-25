package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class SubmissionsResource extends AbstractCubeResource {

    public String handleGet() throws JsonProcessingException {

        List<Submission> submissions = submissionStore.getAllSubmissions();

        return MAPPER.writeValueAsString(ImmutableMap.of("submissions", submissions));
    }

    public String handlePost(JsonRepresentation representation) throws JsonProcessingException {
        try {
            JSONObject obj = representation.getJsonObject();
            String teamId = obj.getString("teamId");
            String puzzleId = obj.getString("puzzleId");
            String submission = obj.getString("submission");

            String visibilityStatus = huntStatusStore.getVisibility(teamId, puzzleId);
            if (!huntStatusStore.getVisibilityStatusSet().allowsSubmissions(visibilityStatus)) {
                return MAPPER.writeValueAsString(ImmutableMap.of("created", false));
            }

            boolean success = submissionStore.addSubmission(teamId, puzzleId, submission);
            return MAPPER.writeValueAsString(ImmutableMap.of("created", success));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
