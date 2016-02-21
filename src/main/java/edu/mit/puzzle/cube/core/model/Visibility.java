package edu.mit.puzzle.cube.core.model;

import java.time.Instant;

public class Visibility {

    private String teamId;
    private String puzzleId;
    private String status;

    public Visibility(String teamId, String puzzleId, String status) {
        this.teamId = teamId;
        this.puzzleId = puzzleId;
        this.status = status;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getPuzzleId() {
        return puzzleId;
    }

    public String getStatus() {
        return status;
    }
}
