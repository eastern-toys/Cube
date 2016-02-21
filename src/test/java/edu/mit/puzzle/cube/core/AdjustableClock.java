package edu.mit.puzzle.cube.core;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Function;

public class AdjustableClock extends Clock {

    private Clock wrappedClock;

    public AdjustableClock(Clock clock) {
        this.wrappedClock = clock;
    }

    public Clock getWrappedClock() {
        return this.wrappedClock;
    }

    public void setWrappedClock(Clock clock) {
        this.wrappedClock = clock;
    }

    public void adjustClock(Function<Clock,Clock> adjustment) {
        this.wrappedClock = adjustment.apply(this.wrappedClock);
    }

    @Override
    public ZoneId getZone() {
        return this.wrappedClock.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this.wrappedClock.withZone(zone);
    }

    @Override
    public Instant instant() {
        return this.wrappedClock.instant();
    }

}
