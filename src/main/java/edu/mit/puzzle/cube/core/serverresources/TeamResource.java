package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.Team;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;

public class TeamResource extends AbstractCubeResource {

    private String getId() {
        String idString = (String) getRequest().getAttributes().get("id");
        if (idString == null) {
            throw new IllegalArgumentException("id must be specified");
        }
        return idString;
    }

    @Get
    public Team handleGet() {
        String id = getId();
        SecurityUtils.getSubject().checkPermission("teaminfo:read:" + id);
        return huntStatusStore.getTeam(id);
    }
}
