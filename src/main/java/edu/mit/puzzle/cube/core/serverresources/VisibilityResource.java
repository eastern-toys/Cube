package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.model.Visibility;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public class VisibilityResource extends AbstractCubeResource {

    private String getTeamId() {
        String idString = (String) getRequest().getAttributes().get("teamId");
        if (idString == null) {
            throw new IllegalArgumentException("teamId must be specified");
        }
        return idString;
    }

    private String getPuzzleId() {
        String idString = (String) getRequest().getAttributes().get("puzzleId");
        if (idString == null) {
            throw new IllegalArgumentException("puzzleId must be specified");
        }
        return idString;
    }

    @Get
    public Visibility handleGet() throws JsonProcessingException {
        String teamId = getTeamId();
        String puzzleId = getPuzzleId();
        return Visibility.builder()
                .setTeamId(teamId)
                .setPuzzleId(puzzleId)
                .setStatus(huntStatusStore.getVisibility(teamId, puzzleId))
                .build();
    }

    @Post
    public Representation handlePost(Visibility visibility) throws JsonProcessingException {
        String teamId = getTeamId();
        String puzzleId = getPuzzleId();

        if (visibility.getStatus() == null
                || !huntStatusStore.getVisibilityStatusSet().isAllowedStatus(visibility.getStatus())) {
            return new JsonRepresentation(MAPPER.writeValueAsString(ImmutableMap.of("updated", false)));
        }

        boolean changed = huntStatusStore.setVisibility(teamId, puzzleId, visibility.getStatus(), true);
        return new JsonRepresentation(MAPPER.writeValueAsString(ImmutableMap.of("updated",changed)));
    }
}
