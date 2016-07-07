package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.AdjustableClock;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.InMemoryConnectionFactory;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class HuntStatusStoreTest {

    private ConnectionFactory connectionFactory;
    private AdjustableClock clock;
    private VisibilityStatusSet visibilityStatusSet;
    private HuntStatusStore huntStatusStore;
    private EventProcessor<Event> eventProcessor;

    private static String TEST_TEAM_ID = "testerteam";
    private static String TEST_PUZZLE_ID = "a_test_puzzle";
    private static String TEST_PUZZLE_ID_2 = "another_test_puzzle";
    private static String TEST_PUZZLE_ID_3 = "yet_another_test_puzzle";

    @Before
    public void setup() throws SQLException {
        visibilityStatusSet = new StandardVisibilityStatusSet();
        connectionFactory = new InMemoryConnectionFactory(
                visibilityStatusSet,
                Lists.newArrayList(TEST_TEAM_ID),
                Lists.newArrayList(TEST_PUZZLE_ID,TEST_PUZZLE_ID_2,TEST_PUZZLE_ID_3),
                ImmutableList.<User>of());
        clock = new AdjustableClock(Clock.fixed(Instant.now(), ZoneId.of("UTC")));
        eventProcessor = mock(EventProcessor.class);
        huntStatusStore = new HuntStatusStore(connectionFactory, clock, visibilityStatusSet, eventProcessor);
    }

    @Test
    public void getVisibilityWithNoneSet() {
        assertEquals(visibilityStatusSet.getDefaultVisibilityStatus(), huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));
    }

    @Test
    public void setAndGetVisibility() {
        boolean statusChanged = huntStatusStore.setVisibility(
                TEST_TEAM_ID, TEST_PUZZLE_ID, "UNLOCKED", false);
        assertTrue(statusChanged);
        assertEquals("UNLOCKED", huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        List<VisibilityChange> history = huntStatusStore.getVisibilityHistory(TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(1, history.size());
        assertEquals(clock.instant(), history.get(0).getTimestamp());
        assertEquals("UNLOCKED", history.get(0).getStatus());

        verify(eventProcessor, times(1)).process(any(Event.class));
    }

    @Test
    public void setVisibilityToSameStatus() {
        huntStatusStore.setVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID, "UNLOCKED", false);
        assertEquals("UNLOCKED", huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        boolean statusChanged = huntStatusStore.setVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID, "UNLOCKED", false);
        assertFalse(statusChanged);
        assertEquals("UNLOCKED", huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        List<VisibilityChange> history = huntStatusStore.getVisibilityHistory(TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(1, history.size());
        assertEquals(clock.instant(), history.get(0).getTimestamp());
        assertEquals("UNLOCKED", history.get(0).getStatus());

        verify(eventProcessor, times(1)).process(any(Event.class));
    }

    @Test
    public void setVisibilityToLocked() {
        boolean statusChanged = huntStatusStore.setVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID,
                visibilityStatusSet.getDefaultVisibilityStatus(), false);
        assertFalse(statusChanged);
        assertEquals(visibilityStatusSet.getDefaultVisibilityStatus(),
                huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        List<VisibilityChange> history = huntStatusStore.getVisibilityHistory(TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(0, history.size());

        verifyZeroInteractions(eventProcessor);
    }

    @Test
    public void setVisibilityWithIllegalCurrentStatus() {
        boolean statusChanged = huntStatusStore.setVisibility(
                TEST_TEAM_ID, TEST_PUZZLE_ID, "SOLVED", false);
        assertFalse(statusChanged);
        assertEquals(visibilityStatusSet.getDefaultVisibilityStatus(),
                huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        List<VisibilityChange> history = huntStatusStore.getVisibilityHistory(
                TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(0, history.size());

        verifyZeroInteractions(eventProcessor);
    }

    @Test
    public void setVisibilityMultipleTimes() throws InterruptedException {
        boolean statusChanged = huntStatusStore.setVisibility(
                TEST_TEAM_ID, TEST_PUZZLE_ID, "UNLOCKED", false);
        assertTrue(statusChanged);
        assertEquals("UNLOCKED", huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));
        Instant firstTimestamp = clock.instant();

        clock.setWrappedClock(Clock.fixed(clock.instant().plus(5, ChronoUnit.MINUTES), ZoneId.of("UTC")));
        Instant secondTimestamp = clock.instant();

        statusChanged = huntStatusStore.setVisibility(
                TEST_TEAM_ID, TEST_PUZZLE_ID, "SOLVED", false);
        assertTrue(statusChanged);
        assertEquals("SOLVED", huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        List<VisibilityChange> history = huntStatusStore.getVisibilityHistory(TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(2, history.size());

        assertEquals(firstTimestamp, history.get(0).getTimestamp());
        assertEquals("UNLOCKED", history.get(0).getStatus());
        assertEquals(secondTimestamp, history.get(1).getTimestamp());
        assertEquals("SOLVED", history.get(1).getStatus());

        verify(eventProcessor, times(2)).process(any(Event.class));
    }

    @Test
    public void setMultipleVisibilities() throws InterruptedException {
        huntStatusStore.setVisibility(
                TEST_TEAM_ID, TEST_PUZZLE_ID, "UNLOCKED", false);
        huntStatusStore.setVisibility(
                TEST_TEAM_ID, TEST_PUZZLE_ID, "SOLVED", false);
        huntStatusStore.setVisibility(
                TEST_TEAM_ID, TEST_PUZZLE_ID_2, "UNLOCKED", false);

        List<Visibility> visibilities = huntStatusStore.getVisibilitiesForTeam(TEST_TEAM_ID);
        Visibility.Builder visibilityBuilder = Visibility.builder()
                .setTeamId(TEST_TEAM_ID);
        assertThat(visibilities).containsExactly(
                visibilityBuilder.setPuzzleId(TEST_PUZZLE_ID).setStatus("SOLVED").build(),
                visibilityBuilder.setPuzzleId(TEST_PUZZLE_ID_2).setStatus("UNLOCKED").build(),
                visibilityBuilder.setPuzzleId(TEST_PUZZLE_ID_3).setStatus(visibilityStatusSet.getDefaultVisibilityStatus()).build()
        );
    }

    @AutoValue
    public abstract static class HuntStatusStoreTestProperty extends Team.Property {
        static {
            registerClass(HuntStatusStoreTestProperty.class);
        }

        @JsonCreator
        public static HuntStatusStoreTestProperty create(@JsonProperty("value") String value) {
            return new AutoValue_HuntStatusStoreTest_HuntStatusStoreTestProperty(value);
        }

        @JsonProperty("value") public abstract String getValue();
    }

    @Test
    public void setTeamProperties() {
        assertTrue(huntStatusStore.getTeam(TEST_TEAM_ID).getTeamProperties().isEmpty());
        assertTrue(huntStatusStore.setTeamProperty(
                TEST_TEAM_ID,
                HuntStatusStoreTestProperty.class,
                HuntStatusStoreTestProperty.create("SOME_VALUE")));
        Team team = huntStatusStore.getTeam(TEST_TEAM_ID);
        assertEquals(1, team.getTeamProperties().size());
        assertEquals("SOME_VALUE", team.getTeamProperty(HuntStatusStoreTestProperty.class).getValue());
    }
}
