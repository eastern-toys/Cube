package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Team;
import edu.mit.puzzle.cube.core.model.Teams;
import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.TeamsPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.util.stream.Collectors;

public class TeamsResource extends AbstractCubeResource {

    @Get
    public Teams handleGet() {
        SecurityUtils.getSubject().checkPermission(
                new TeamsPermission("*", PermissionAction.READ));
        return Teams.builder()
                .setTeams(huntStatusStore.getTeamIds().stream()
                        .map(teamId -> Team.builder()
                                .setTeamId(teamId)
                                // TODO: include team properties in this response
                                .setTeamProperties(ImmutableMap.<String, Team.Property>of())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Post
    public PostResult handlePost(Team team) {
        SecurityUtils.getSubject().checkPermission(
                new TeamsPermission(team.getTeamId(), PermissionAction.CREATE));
        Preconditions.checkArgument(
                !team.getTeamId().isEmpty(),
                "The team id must be non-empty"
        );
        Preconditions.checkArgument(
                team.getPassword() != null && !team.getPassword().isEmpty(),
                "A password must be provided when creating a team"
        );

        huntStatusStore.addTeam(team);

        User user = User.builder()
                .setUsername(team.getTeamId())
                .setPassword(team.getPassword())
                .setTeamId(team.getTeamId())
                .build();
        userStore.addUser(user);

        return PostResult.builder().setCreated(true).build();
    }
}
