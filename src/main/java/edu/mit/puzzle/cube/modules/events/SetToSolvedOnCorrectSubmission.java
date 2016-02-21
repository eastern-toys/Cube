package edu.mit.puzzle.cube.modules.events;

import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.SubmissionCompleteEvent;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;

import static com.google.common.base.Preconditions.checkNotNull;

public class SetToSolvedOnCorrectSubmission implements EventProcessor {

    private final HuntStatusStore huntStatusStore;

    public SetToSolvedOnCorrectSubmission(
            HuntStatusStore huntStatusStore
    ) {
        this.huntStatusStore = checkNotNull(huntStatusStore);
    }

    @Override
    public void process(Event event) {
        if (SubmissionCompleteEvent.class.isInstance(event)) {
            SubmissionCompleteEvent scEvent = (SubmissionCompleteEvent) event;
            Submission submission = scEvent.getSubmission();
            if (submission.getStatus().equals(SubmissionStatus.CORRECT)) {
                huntStatusStore.setVisibility(
                        submission.getTeamId(),
                        submission.getPuzzleId(),
                        "SOLVED",
                        false
                );
            }
        }
    }

}
