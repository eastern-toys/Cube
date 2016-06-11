package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.VisibilityChanges;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;

public class VisibilityChangesResource extends AbstractCubeResource {

    // TODO: introduce some filtering and/or pagination on this API - always reading all
    // visibility changes may not scale.
    @Get
    public VisibilityChanges handleGet() {
        SecurityUtils.getSubject().checkPermission("visibilities:read");
        return VisibilityChanges.builder()
                .setVisibilityChanges(huntStatusStore.getVisibilityChanges())
                .build();
    }
}
