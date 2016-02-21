package edu.mit.puzzle.cube.huntimpl.huntception;

import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.events.AbstractUnlockUpdateCalculator;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class HuntceptionUnlockUpdateCalculator extends AbstractUnlockUpdateCalculator {

    public HuntceptionUnlockUpdateCalculator(VisibilityStatusSet visibilityStatusSet, Map<String, Object> currentHuntRunProperties, Map<String, Object> currentTeamUnlockProperties, Map<String, String> currentTeamVisibilities) {
        super(visibilityStatusSet, currentHuntRunProperties, currentTeamUnlockProperties, currentTeamVisibilities);
    }

    ImmutableMap<String,Supplier<Boolean>> PUZZLES_BECOME_UNLOCKED_WHEN =
            ImmutableMap.<String,Supplier<Boolean>>builder()
                    .put("dog_show", () -> whenHuntStarts())
                    .put("you_complete_me", () -> whenUnlocked("dog_show"))

                    .put("rip_van_winkle", () -> whenSolved("dog_show"))
                    .put("crimes_against_cruciverbalism", () -> whenUnlocked("rip_van_winkle"))

                    //Doing these releases programmatically is, strictly speaking,
                    //unnecessary because you could accomplish the same thing through
                    //manual FullRelease events. But it's useful to show a custom
                    //unlock pattern.
                    .put("dreamtime", () -> whenAfterDreamtime1Release(0))
                    .put("dreamtime_day_one", () -> whenAfterDreamtime1Release(0))
                    .put("dreamtime_day_two", () -> whenAfterDreamtime1Release(30))
                    .put("dreamtime_day_three", () -> whenAfterDreamtime1Release(60))
                    .build();

    @Override
    protected Map<String, Supplier<Boolean>> getMapOfWhenStatusOccurs(String status) {
        if (status.equals("UNLOCKED")) {
            return PUZZLES_BECOME_UNLOCKED_WHEN;
        } else {
            return ImmutableMap.of();
        }
    }

    //This is also overly fancy because we'd really just set a time for this directly because
    //we'd know what the Saturday of Hunt is.
    private Optional<Instant> getDreamtime1ReleaseTime() {
        //If the hunt hasn't started, don't release Dreamtime
        Optional<Instant> huntStartTimestamp = Optional.of(
                (Instant) currentHuntRunProperties.get("startTimestamp"));
        if (!huntStartTimestamp.isPresent()) {
            return Optional.empty();
        }

        //This is getting the date that the hunt run started in Eastern Time
        ZonedDateTime startTime = huntStartTimestamp.get().atZone(ZoneId.of("America/New_York"));
        //This is local noon
        LocalTime localReleaseTime = LocalTime.of(12, 0);
        //This is noon the day after the hunt run started
        ZonedDateTime releaseTime = ZonedDateTime.of(startTime.toLocalDate().plusDays(1), localReleaseTime, startTime.getZone());

        return Optional.of(releaseTime.toInstant());
    }

    private boolean whenAfterDreamtime1Release(int delayInMinutes) {
        Optional<Instant> dreamtime1ReleaseTime = getDreamtime1ReleaseTime();
        if (dreamtime1ReleaseTime.isPresent()) {
            return Instant.now().isAfter(dreamtime1ReleaseTime.get().plus(delayInMinutes, ChronoUnit.MINUTES));
        } else {
            //Don't release if hunt hasn't started (and therefore there's no set time)
            return false;
        }
    }

    @Override
    public Map<String, Object> getTeamPropertyUpdates() {
        return ImmutableMap.of();
    }
}
