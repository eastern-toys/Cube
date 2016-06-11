package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.Visibilities;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;

import java.util.Optional;

public class VisibilitiesResource extends AbstractCubeResource {

    @Get
    public Visibilities handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission("visibilities:read:" + teamId.get());
        } else {
            SecurityUtils.getSubject().checkPermission("visibilities:read");
        }
        Optional<String> puzzleId = Optional.ofNullable(getQueryValue("puzzleId"));
        return Visibilities.builder()
                .setVisibilities(huntStatusStore.getExplicitVisibilities(teamId, puzzleId))
                .build();
    }
}
