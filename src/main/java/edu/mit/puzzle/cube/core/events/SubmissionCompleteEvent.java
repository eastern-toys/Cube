package edu.mit.puzzle.cube.core.events;

import static com.google.common.base.Preconditions.checkNotNull;

import edu.mit.puzzle.cube.core.model.Submission;

public class SubmissionCompleteEvent extends Event {

    public static final String EVENT_TYPE = "SubmissionComplete";

    private final Submission submission;

    public SubmissionCompleteEvent(Submission submission) {
        super(EVENT_TYPE);
        this.submission = checkNotNull(submission);
    }

    public Submission getSubmission() {
        return submission;
    }
}
