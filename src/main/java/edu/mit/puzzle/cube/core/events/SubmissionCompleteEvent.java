package edu.mit.puzzle.cube.core.events;

import static com.google.common.base.Preconditions.checkNotNull;

import edu.mit.puzzle.cube.core.model.Submission;

public class SubmissionCompleteEvent implements Event {

    public static final String EVENT_TYPE = "SubmissionComplete";

    private final Submission submission;

    public SubmissionCompleteEvent(Submission submission) {
        this.submission = checkNotNull(submission);
    }

    public Submission getSubmission() {
        return submission;
    }
}