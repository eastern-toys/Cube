package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.mit.puzzle.cube.core.model.Visibilities;
import org.restlet.resource.Get;
import java.util.Optional;

public class VisibilitiesResource extends AbstractCubeResource {

    @Get
    public Visibilities handleGet() throws JsonProcessingException {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        Optional<String> puzzleId = Optional.ofNullable(getQueryValue("puzzleId"));
        return Visibilities.builder()
                .setVisibilities(huntStatusStore.getExplicitVisibilities(teamId, puzzleId))
                .build();
    }
}
