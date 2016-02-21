package edu.mit.puzzle.cube.core.events;

import edu.mit.puzzle.cube.core.model.Submission;

import static com.google.common.base.Preconditions.checkNotNull;

public class SubmissionCompleteEvent implements Event {

    private final Submission submission;

    public SubmissionCompleteEvent(Submission submission) {
        this.submission = checkNotNull(submission);
    }

    @Override
    public boolean isExternallyInitiated() {
        return true;
    }

    public Submission getSubmission() {
        return this.submission;
    }
}
