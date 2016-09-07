package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(FullReleaseEvent.class),
    @JsonSubTypes.Type(HintCompleteEvent.class),
    @JsonSubTypes.Type(HuntStartEvent.class),
    @JsonSubTypes.Type(PeriodicTimerEvent.class),
    @JsonSubTypes.Type(SubmissionCompleteEvent.class),
    @JsonSubTypes.Type(VisibilityChangeEvent.class),
})
public abstract class Event {
}
