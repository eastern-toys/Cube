package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.model.Visibility;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import org.restlet.ext.json.JsonRepresentation;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class VisibilitiesResource extends AbstractCubeResource {

    private HuntStatusStore huntStatusStore;

    public VisibilitiesResource(
            HuntStatusStore huntStatusStore
    ) {
        this.huntStatusStore = checkNotNull(huntStatusStore);
    }

    public String handleGet() throws JsonProcessingException {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        Optional<String> puzzleId = Optional.ofNullable(getQueryValue("puzzleId"));

        List<Visibility> visibilities = huntStatusStore.getExplicitVisibilities(teamId, puzzleId);

        return MAPPER.writeValueAsString(ImmutableMap.of("visibilities", visibilities));
    }

    public String handlePost(JsonRepresentation representation) throws JsonProcessingException {
        throw new UnsupportedOperationException();
    }

}
