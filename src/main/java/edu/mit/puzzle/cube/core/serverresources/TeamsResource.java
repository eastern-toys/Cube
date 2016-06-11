package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.collect.ImmutableMap;

import edu.mit.puzzle.cube.core.model.Team;
import edu.mit.puzzle.cube.core.model.Teams;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;

import java.util.stream.Collectors;

public class TeamsResource extends AbstractCubeResource {

    @Get
    public Teams handleGet() {
        SecurityUtils.getSubject().checkPermission("teams:read");
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
}
