package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.model.HintRequestStore;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.PuzzleStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.UserStore;

import org.restlet.resource.ServerResource;

public abstract class AbstractCubeResource extends ServerResource {

    protected static ObjectMapper MAPPER = new ObjectMapper();

    public static final String SUBMISSION_STORE_KEY = "SUBMISSION_STORE";
    public static final String HUNT_STATUS_STORE_KEY = "HUNT_STATUS_STORE";
    public static final String USER_STORE_KEY = "USER_STORE";
    public static final String PUZZLE_STORE_KEY = "PUZZLE_STORE";
    public static final String HINT_REQUEST_STORE_KEY = "HINT_REQUEST_STORE";
    public static final String EVENT_PROCESSOR_KEY = "EVENT_PROCESSOR";

    protected SubmissionStore submissionStore;
    protected HuntStatusStore huntStatusStore;
    protected UserStore userStore;
    protected PuzzleStore puzzleStore;
    protected HintRequestStore hintRequestStore;
    protected EventProcessor<Event> eventProcessor;

    public AbstractCubeResource() {
    }

    public void doInit() {
        this.submissionStore = (SubmissionStore) getContext().getAttributes().get(SUBMISSION_STORE_KEY);
        this.huntStatusStore = (HuntStatusStore) getContext().getAttributes().get(HUNT_STATUS_STORE_KEY);
        this.userStore = (UserStore) getContext().getAttributes().get(USER_STORE_KEY);
        this.puzzleStore = (PuzzleStore) getContext().getAttributes().get(PUZZLE_STORE_KEY);
        this.hintRequestStore = (HintRequestStore) getContext().getAttributes().get(HINT_REQUEST_STORE_KEY);
        this.eventProcessor = (EventProcessor<Event>) getContext().getAttributes().get(EVENT_PROCESSOR_KEY);
    }
}
