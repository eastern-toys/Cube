package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.Visibilities;
import edu.mit.puzzle.cube.core.model.Visibility;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.VisibilitiesPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class VisibilitiesResource extends AbstractCubeResource {

    @Get
    public Visibilities handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new VisibilitiesPermission(teamId.get(), PermissionAction.READ));
        } else {
            SecurityUtils.getSubject().checkPermission(
                    new VisibilitiesPermission("*", PermissionAction.READ));
        }
        Optional<String> puzzleId = Optional.ofNullable(getQueryValue("puzzleId"));

        List<Visibility> visibilities = huntStatusStore.getExplicitVisibilities(teamId, puzzleId);
        visibilities = visibilities.stream()
                .map(visibility -> visibility.toBuilder()
                        .setPuzzleDisplayName(
                                puzzleStore.getPuzzle(visibility.getPuzzleId()).getDisplayName())
                        .build()
                )
                .collect(Collectors.toList());
        return Visibilities.builder()
                .setVisibilities(visibilities)
                .build();
    }
}
