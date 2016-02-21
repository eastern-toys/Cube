package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import org.restlet.ext.json.JsonRepresentation;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class TeamResource extends AbstractCubeResource {

    private HuntStatusStore huntStatusStore;

    public TeamResource(
        HuntStatusStore huntStatusStore
    ) {
        this.huntStatusStore = checkNotNull(huntStatusStore);
    }

    private String getId() {
        String idString = (String) getRequest().getAttributes().get("id");
        if (idString == null) {
            throw new IllegalArgumentException("id must be specified");
        }
        return idString;
    }

    public String handleGet() throws JsonProcessingException {
        String id = getId();
        Map<String,Object> propertyMap = huntStatusStore.getTeamProperties(id);

        Map<String,Object> returnMap = Maps.newHashMap();
        returnMap.put("teamId",id);
        returnMap.putAll(propertyMap);

        return MAPPER.writeValueAsString(returnMap);
    }

    public String handlePost(JsonRepresentation representation) throws JsonProcessingException {
        return "";
    }

}
