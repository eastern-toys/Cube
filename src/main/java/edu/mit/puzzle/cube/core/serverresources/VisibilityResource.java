package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

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

    @Override
    protected String handleGet() throws JsonProcessingException {
        String teamId = getTeamId();
        String puzzleId = getPuzzleId();

        Map<String,Object> defaultMap = Maps.newHashMap();
        defaultMap.put("teamId", teamId);
        defaultMap.put("puzzleId", puzzleId);
        defaultMap.put("status", huntStatusStore.getVisibility(teamId, puzzleId));
        return MAPPER.writeValueAsString(defaultMap);
    }

    @Override
    protected String handlePost(JsonRepresentation representation) throws JsonProcessingException {
        String teamId = getTeamId();
        String puzzleId = getPuzzleId();

        try {
            JSONObject obj = representation.getJsonObject();
            String status = obj.getString("status");
            if (status == null || !huntStatusStore.getVisibilityStatusSet().isAllowedStatus(status)) {
                return MAPPER.writeValueAsString(ImmutableMap.of("updated", false));
            }

            boolean changed = huntStatusStore.setVisibility(teamId, puzzleId, status, true);
            return MAPPER.writeValueAsString(ImmutableMap.of("updated",changed));

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
}
