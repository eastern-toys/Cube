package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import java.util.Map;

public class TeamResource extends AbstractCubeResource {

    private String getId() {
        String idString = (String) getRequest().getAttributes().get("id");
        if (idString == null) {
            throw new IllegalArgumentException("id must be specified");
        }
        return idString;
    }

    @Get
    public Representation handleGet() throws JsonProcessingException {
        String id = getId();
        Map<String,Object> propertyMap = huntStatusStore.getTeamProperties(id);

        Map<String,Object> returnMap = Maps.newHashMap();
        returnMap.put("teamId",id);
        returnMap.putAll(propertyMap);

        return new JsonRepresentation(MAPPER.writeValueAsString(returnMap));
    }
}
