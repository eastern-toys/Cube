package edu.mit.puzzle.cube.huntimpl.hintexample;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.FullReleaseEvent;
import edu.mit.puzzle.cube.core.events.HintCompleteEvent;
import edu.mit.puzzle.cube.core.events.HuntStartEvent;
import edu.mit.puzzle.cube.core.events.SubmissionCompleteEvent;
import edu.mit.puzzle.cube.core.events.VisibilityChangeEvent;
import edu.mit.puzzle.cube.core.model.HintRequest;
import edu.mit.puzzle.cube.core.model.HintRequestStatus;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.Team;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HintExampleHuntDefinition implements HuntDefinition {
    private static final VisibilityStatusSet VISIBILITY_STATUS_SET = new StandardVisibilityStatusSet();

    @AutoValue
    public abstract static class HintTokensProperty extends Team.Property {
        static {
            registerClass(HintTokensProperty.class);
        }

        @JsonCreator
        public static HintTokensProperty create(@JsonProperty("tokens") int tokens) {
            return new AutoValue_HintExampleHuntDefinition_HintTokensProperty(tokens);
        }

        @JsonProperty("tokens") public abstract int getTokens();
    }

    @Override
    public VisibilityStatusSet getVisibilityStatusSet() {
        return VISIBILITY_STATUS_SET;
    }

    @Override
    public List<Puzzle> getPuzzles() {
        return ImmutableList.of(
                Puzzle.create("puzzle1", "ANSWER1"),
                Puzzle.create("puzzle2", "ANSWER2"),
                Puzzle.create("puzzle3", "ANSWER3"),
                Puzzle.create("meta", "ANSWERMETA")
        );
    }

    @Override
    public void addToEventProcessor(CompositeEventProcessor eventProcessor, HuntStatusStore huntStatusStore) {
        eventProcessor.addEventProcessor(HuntStartEvent.class, event -> {
            boolean changed = huntStatusStore.recordHuntRunStart();
            if (changed) {
                for (String teamId : huntStatusStore.getTeamIds()) {
                    // Everyone starts with all of the round puzzles unlocked. We'll unlock the meta
                    // after they solve at least one puzzle.
                    huntStatusStore.setVisibility(teamId, "puzzle1", "UNLOCKED", false);
                    huntStatusStore.setVisibility(teamId, "puzzle2", "UNLOCKED", false);
                    huntStatusStore.setVisibility(teamId, "puzzle3", "UNLOCKED", false);

                    // Everyone starts with one hint token.
                    huntStatusStore.setTeamProperty(
                            teamId,
                            HintTokensProperty.class,
                            HintTokensProperty.create(1));
                }
            }
        });

        eventProcessor.addEventProcessor(SubmissionCompleteEvent.class, event -> {
            Submission submission = event.getSubmission();
            if (submission.getStatus().equals(SubmissionStatus.CORRECT)) {
                huntStatusStore.setVisibility(
                        submission.getTeamId(),
                        submission.getPuzzleId(),
                        "SOLVED",
                        false
                );
                // Credit a hint token every time a team solves a puzzle.
                huntStatusStore.mutateTeamProperty(
                        submission.getTeamId(),
                        HintTokensProperty.class,
                        hintTokensProperty -> HintTokensProperty.create(hintTokensProperty.getTokens() + 1)
                );
            }
        });

        eventProcessor.addEventProcessor(VisibilityChangeEvent.class, event -> {
            String teamId = event.getVisibility().getTeamId();
            String puzzleId = event.getVisibility().getPuzzleId();
            String status = event.getVisibility().getStatus();

            if (status.equals("SOLVED") && puzzleId.startsWith("puzzle")) {
                // We'll unlock the meta after a team solves at least one round puzzle.
                huntStatusStore.setVisibility(teamId, "meta", "UNLOCKED", false);
            }
        });

        eventProcessor.addEventProcessor(FullReleaseEvent.class, event -> {
            for (String teamId : huntStatusStore.getTeamIds()) {
                huntStatusStore.setVisibility(
                        teamId,
                        event.getPuzzleId(),
                        "UNLOCKED",
                        false
                );
            }
        });

        eventProcessor.addEventProcessor(HintCompleteEvent.class, event -> {
            String teamId = event.getHintRequest().getTeamId();
            HintRequestStatus hintRequestStatus = event.getHintRequest().getStatus();
            if (hintRequestStatus == HintRequestStatus.REJECTED) {
                // Refund the team their token back.
                huntStatusStore.mutateTeamProperty(
                        teamId,
                        HintTokensProperty.class,
                        hintTokensProperty -> HintTokensProperty.create(hintTokensProperty.getTokens() + 1)
                );
            }
        });
    }

    @Override
    public boolean handleHintRequest(HintRequest hintRequest, HuntStatusStore huntStatusStore) {
        // Don't allow hints on the meta.
        if (hintRequest.getPuzzleId().equals("meta")) {
            return false;
        }
        AtomicBoolean deductedToken = new AtomicBoolean(false);
        huntStatusStore.mutateTeamProperty(
                hintRequest.getTeamId(),
                HintTokensProperty.class,
                hintTokensProperty -> {
                    if (hintTokensProperty.getTokens() > 0) {
                        deductedToken.set(true);
                        return HintTokensProperty.create(hintTokensProperty.getTokens() - 1);
                    }
                    return hintTokensProperty;
                }
        );
        return deductedToken.get();
    }
}
