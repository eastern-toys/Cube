package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Visibility;

import org.apache.shiro.SecurityUtils;
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
    public Visibility handleGet() {
        String teamId = getTeamId();
        String puzzleId = getPuzzleId();
        SecurityUtils.getSubject().checkPermission("visibilities:read:" + teamId);
        return Visibility.builder()
                .setTeamId(teamId)
                .setPuzzleId(puzzleId)
                .setStatus(huntStatusStore.getVisibility(teamId, puzzleId))
                .build();
    }

    @Post
    public PostResult handlePost(Visibility visibility) {
        String teamId = getTeamId();
        String puzzleId = getPuzzleId();

        SecurityUtils.getSubject().checkPermission("visibilities:update:" + teamId);

        if (visibility.getStatus() == null
                || !huntStatusStore.getVisibilityStatusSet().isAllowedStatus(visibility.getStatus())) {
            return PostResult.builder().setUpdated(false).build();
        }

        boolean changed = huntStatusStore.setVisibility(teamId, puzzleId, visibility.getStatus(), true);
        return PostResult.builder().setUpdated(changed).build();
    }
}
