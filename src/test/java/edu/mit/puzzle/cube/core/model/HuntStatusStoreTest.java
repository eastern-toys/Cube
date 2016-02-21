package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.*;
import edu.mit.puzzle.cube.core.AdjustableClock;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.InMemorySingleUnsharedConnectionFactory;
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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class HuntStatusStoreTest {

    private ConnectionFactory connectionFactory;
    private AdjustableClock clock;
    private VisibilityStatusSet visibilityStatusSet;
    private HuntStatusStore huntStatusStore;
    private EventProcessor eventProcessor;

    private static String TEST_TEAM_ID = "testerteam";
    private static String TEST_PUZZLE_ID = "a_test_puzzle";
    private static String TEST_PUZZLE_ID_2 = "another_test_puzzle";
    private static String TEST_PUZZLE_ID_3 = "yet_another_test_puzzle";

    @Before
    public void setup() throws SQLException {
        visibilityStatusSet = new StandardVisibilityStatusSet();
        connectionFactory = new InMemorySingleUnsharedConnectionFactory(
                visibilityStatusSet,
                Lists.newArrayList(TEST_TEAM_ID),
                Lists.newArrayList(TEST_PUZZLE_ID,TEST_PUZZLE_ID_2,TEST_PUZZLE_ID_3));
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

        Table<Integer,String,Object> history = huntStatusStore.getVisibilityHistory(TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(1, history.rowKeySet().size());
        assertEquals(clock.instant(), history.get(0, "timestamp"));
        assertEquals("UNLOCKED", history.get(0, "status"));

        verify(eventProcessor, times(1)).process(any(Event.class));
    }

    @Test
    public void setVisibilityToSameStatus() {
        huntStatusStore.setVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID, "UNLOCKED", false);
        assertEquals("UNLOCKED", huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        boolean statusChanged = huntStatusStore.setVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID, "UNLOCKED", false);
        assertFalse(statusChanged);
        assertEquals("UNLOCKED", huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        Table<Integer,String,Object> history = huntStatusStore.getVisibilityHistory(TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(1, history.rowKeySet().size());
        assertEquals(clock.instant(), history.get(0, "timestamp"));
        assertEquals("UNLOCKED", history.get(0, "status"));

        verify(eventProcessor, times(1)).process(any(Event.class));
    }

    @Test
    public void setVisibilityToLocked() {
        boolean statusChanged = huntStatusStore.setVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID,
                visibilityStatusSet.getDefaultVisibilityStatus(), false);
        assertFalse(statusChanged);
        assertEquals(visibilityStatusSet.getDefaultVisibilityStatus(),
                huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        Table<Integer,String,Object> history = huntStatusStore.getVisibilityHistory(TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(0, history.rowKeySet().size());

        verifyZeroInteractions(eventProcessor);
    }

    @Test
    public void setVisibilityWithIllegalCurrentStatus() {
        boolean statusChanged = huntStatusStore.setVisibility(
                TEST_TEAM_ID, TEST_PUZZLE_ID, "SOLVED", false);
        assertFalse(statusChanged);
        assertEquals(visibilityStatusSet.getDefaultVisibilityStatus(),
                huntStatusStore.getVisibility(TEST_TEAM_ID, TEST_PUZZLE_ID));

        Table<Integer,String,Object> history = huntStatusStore.getVisibilityHistory(
                TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(0, history.rowKeySet().size());

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

        Table<Integer,String,Object> history = huntStatusStore.getVisibilityHistory(TEST_TEAM_ID, TEST_PUZZLE_ID);
        assertEquals(2, history.rowKeySet().size());

        assertEquals(firstTimestamp, history.get(0, "timestamp"));
        assertEquals("UNLOCKED", history.get(0, "status"));
        assertEquals(secondTimestamp, history.get(1, "timestamp"));
        assertEquals("SOLVED", history.get(1, "status"));

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

        Map<String,String> visibilities = huntStatusStore.getVisibilitiesForTeam(TEST_TEAM_ID);
        assertEquals(3, visibilities.size());
        assertEquals("SOLVED", visibilities.get(TEST_PUZZLE_ID));
        assertEquals("UNLOCKED", visibilities.get(TEST_PUZZLE_ID_2));
        assertEquals(visibilityStatusSet.getDefaultVisibilityStatus(), visibilities.get(TEST_PUZZLE_ID_3));
    }

    @Test
    public void setProperties() {
        assertTrue(huntStatusStore.getTeamProperties(TEST_TEAM_ID).isEmpty());

        assertTrue(huntStatusStore.setTeamProperty(TEST_TEAM_ID, "GENERIC_PROPERTY", "SOME_VALUE"));
        Map<String,Object> properties = huntStatusStore.getTeamProperties(TEST_TEAM_ID);
        assertEquals(1, properties.size());
        assertEquals("SOME_VALUE", properties.get("GENERIC_PROPERTY"));
    }
}
