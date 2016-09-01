package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.HintRequest;
import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.permissions.HintsPermission;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import java.util.Optional;

public class HintRequestResource extends AbstractCubeResource {

    private int getId() {
        String idString = (String) getRequest().getAttributes().get("id");
        if (idString == null) {
            throw new IllegalArgumentException("id must be specified");
        }
        try {
            return Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id is not valid");
        }
    }

    @Post
    public PostResult handlePost(HintRequest hintRequest) {
        int id = getId();
        if (hintRequest.getStatus() == null) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "A status must be specified when updating a hint request");
        }

        Optional<HintRequest> existingHintRequest = hintRequestStore.getHintRequest(id);
        if (!existingHintRequest.isPresent()) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_NOT_FOUND,
                    String.format("Hint request %d does not exist", id));
        }

        Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(
                new HintsPermission(existingHintRequest.get().getTeamId(), PermissionAction.UPDATE));

        if (existingHintRequest.get().getStatus().isTerminal()) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "This hint request has already been handled and may no longer be changed.");
        }

        String currentUsername = (String) subject.getPrincipal();

        if (existingHintRequest.get().getStatus().isAssigned()
                && hintRequest.getStatus().isAssigned()
                && !existingHintRequest.get().getCallerUsername().equals(currentUsername)) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    String.format(
                            "This hint request is already claimed by %s. It must be unassigned " +
                            "before you can change it.",
                            existingHintRequest.get().getCallerUsername()));
        }

        String callerUsername = null;
        if (hintRequest.getStatus().isAssigned()) {
            callerUsername = currentUsername;
        }

        boolean changed = hintRequestStore.updateHintRequest(
                id, hintRequest.getStatus(), callerUsername, hintRequest.getResponse());
        return PostResult.builder().setUpdated(changed).build();
    }
}
