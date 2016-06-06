package edu.mit.puzzle.cube.huntimpl.scoreexample;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.FullReleaseEvent;
import edu.mit.puzzle.cube.core.events.HuntStartEvent;
import edu.mit.puzzle.cube.core.events.PeriodicTimerEvent;
import edu.mit.puzzle.cube.core.events.SubmissionCompleteEvent;
import edu.mit.puzzle.cube.core.events.VisibilityChangeEvent;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.Team;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScoreExampleHuntDefinition implements HuntDefinition {

    private static final VisibilityStatusSet VISIBILITY_STATUS_SET = new StandardVisibilityStatusSet();
    private static final Map<String,PuzzleInfo> PUZZLE_INFO_MAP;
    static {
        ImmutableMap.Builder<String,PuzzleInfo> puzzleInfoBuilder = ImmutableMap.builder();
        for (int i = 1; i <= 7; ++i) {
            int reward = 25;
            int prereq = (i-1) * 20;
            puzzleInfoBuilder.put("puzzle" + i, new PuzzleInfo(reward, prereq));
        }
        PUZZLE_INFO_MAP = puzzleInfoBuilder.build();
    }

    private static class PuzzleInfo {
        public int pointReward;
        public int pointPrereq;
        public PuzzleInfo(int pointReward, int pointPrereq) {
            this.pointReward = pointReward;
            this.pointPrereq = pointPrereq;
        }
    }

    @AutoValue
    public abstract static class ScoreProperty extends Team.Property {
        static {
            registerClass(ScoreProperty.class);
        }

        @JsonCreator
        public static ScoreProperty create(@JsonProperty("score") int score) {
            return new AutoValue_ScoreExampleHuntDefinition_ScoreProperty(score);
        }

        @JsonProperty("score") public abstract int getScore();
    }

    @Override
    public VisibilityStatusSet getVisibilityStatusSet() {
        return VISIBILITY_STATUS_SET;
    }

    @Override
    public List<String> getPuzzleList() {
        return Lists.newArrayList(PUZZLE_INFO_MAP.keySet());
    }

    @Override
    public void addToEventProcessor(
            CompositeEventProcessor eventProcessor,
            HuntStatusStore huntStatusStore
    ) {
        eventProcessor.addEventProcessor(SubmissionCompleteEvent.class, event -> {
            Submission submission = event.getSubmission();
            if (submission.getStatus().equals(SubmissionStatus.CORRECT)) {
                huntStatusStore.setVisibility(
                        submission.getTeamId(),
                        submission.getPuzzleId(),
                        "SOLVED",
                        false
                );
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

        eventProcessor.addEventProcessor(HuntStartEvent.class, event -> {
            boolean changed = huntStatusStore.recordHuntRunStart();
            if (changed) {
                for (String teamId : huntStatusStore.getTeamIds()) {
                    huntStatusStore.setTeamProperty(
                            teamId,
                            ScoreProperty.class,
                            ScoreProperty.create(0));
                }
            }
        });

        eventProcessor.addEventProcessor(VisibilityChangeEvent.class, event -> {
            updateStoredScore(event.getVisibility().getTeamId(), huntStatusStore, eventProcessor);
        });

        eventProcessor.addEventProcessor(ScoreUpdateEvent.class, event -> {
            PUZZLE_INFO_MAP.entrySet().stream()
                    .filter(puzzleEntry -> event.getScore() >= puzzleEntry.getValue().pointPrereq)
                    .map(Map.Entry::getKey)
                    .forEach(puzzleKey -> huntStatusStore.setVisibility(event.getTeamId(), puzzleKey, "UNLOCKED", false));
        });

        eventProcessor.addEventProcessor(PeriodicTimerEvent.class, event -> {
            for (String teamId : huntStatusStore.getTeamIds()) {
                updateStoredScore(teamId, huntStatusStore, eventProcessor);
            }
        });
    }

    private void updateStoredScore(
            String teamId,
            HuntStatusStore huntStatusStore,
            EventProcessor<Event> eventProcessor
    ) {
        Optional<Integer> score = calculateTeamScore(teamId, huntStatusStore);
        if (score.isPresent()) {
            huntStatusStore.setTeamProperty(
                    teamId,
                    ScoreProperty.class,
                    ScoreProperty.create(score.get()));
            eventProcessor.process(ScoreUpdateEvent.builder()
                    .setTeamId(teamId)
                    .setScore(score.get())
                    .build());
        }
    }

    private Optional<Integer> calculateTeamScore(
            String teamId,
            HuntStatusStore huntStatusStore
    ) {
        Optional<Instant> start = Optional.ofNullable(
                (Instant) huntStatusStore.getHuntRunProperties().get("startTimestamp"));
        if (!start.isPresent() || Instant.now().isBefore(start.get())) {
            return Optional.empty();
        }

        int seconds = (int) Duration.between(start.get(), Instant.now()).getSeconds();
        int timeScore = seconds / 60; //1 point every minute

        Map<String, String> visibilities = huntStatusStore.getVisibilitiesForTeam(teamId);
        int puzzleScore = visibilities.entrySet().stream()
                .filter(entry -> entry.getValue().equals("SOLVED"))
                .map(entry -> entry.getKey())
                .mapToInt(puzzleKey -> PUZZLE_INFO_MAP.get(puzzleKey).pointReward)
                .sum();

        return Optional.of(timeScore + puzzleScore);
    }

    @AutoValue
    static abstract class ScoreUpdateEvent extends Event {
        @AutoValue.Builder
        static abstract class Builder {
            abstract Builder setTeamId(String teamId);
            abstract Builder setScore(int score);
            abstract ScoreUpdateEvent build();
        }

        static Builder builder() {
            return new AutoValue_ScoreExampleHuntDefinition_ScoreUpdateEvent.Builder();
        }

        abstract String getTeamId();
        abstract int getScore();
    }
}
