package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.*;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;

import java.sql.*;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

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

    public String handleGet() throws JsonProcessingException {
        int id = getId();
        Optional<Submission> submission = submissionStore.getSubmission(id);

        if (submission.isPresent()) {
            return MAPPER.writeValueAsString(submission.get());
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Submission not found");
            return null;
        }
    }

    public String handlePost(JsonRepresentation representation) throws JsonProcessingException {
        int id = getId();
        try {
            JSONObject obj = representation.getJsonObject();
            String statusString = obj.getString("status");
            SubmissionStatus status = SubmissionStatus.valueOf(statusString);
            if (status == null) {
                return MAPPER.writeValueAsString(ImmutableMap.of("updated",false));
            }

            boolean changed = submissionStore.setSubmissionStatus(id, status);
            return MAPPER.writeValueAsString(ImmutableMap.of("updated",changed));

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
