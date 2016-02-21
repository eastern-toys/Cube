package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;

import java.time.Instant;

public class Submission {

    private int submissionId;
    private String teamId;
    private String puzzleId;
    private String submissionText;
    private SubmissionStatus status;
    private Instant timestamp;

    public Submission(int submissionId, String teamId, String puzzleId, String submissionText, SubmissionStatus status, Instant timestamp) {
        this.submissionId = submissionId;
        this.teamId = teamId;
        this.puzzleId = puzzleId;
        this.submissionText = submissionText;
        this.status = status;
        this.timestamp = timestamp;
    }

    public int getSubmissionId() {
        return submissionId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getPuzzleId() {
        return puzzleId;
    }

    public String getSubmissionText() {
        return submissionText;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
