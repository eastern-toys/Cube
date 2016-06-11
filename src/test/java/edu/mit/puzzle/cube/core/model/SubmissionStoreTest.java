package edu.mit.puzzle.cube.core.model;

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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class SubmissionStoreTest {

    private ConnectionFactory connectionFactory;
    private AdjustableClock clock;
    private SubmissionStore submissionStore;
    private EventProcessor<Event> eventProcessor;

    private static String TEST_TEAM_ID = "testerteam";
    private static String TEST_PUZZLE_ID = "a_test_puzzle";

    @Before
    public void setup() throws SQLException {
        connectionFactory = new InMemoryConnectionFactory(
                new StandardVisibilityStatusSet(),
                Lists.newArrayList(TEST_TEAM_ID),
                Lists.newArrayList(TEST_PUZZLE_ID),
                ImmutableList.<User>of());
        clock = new AdjustableClock(Clock.fixed(Instant.now(), ZoneId.of("UTC")));
        eventProcessor = mock(EventProcessor.class);

        submissionStore = new SubmissionStore(connectionFactory, clock, eventProcessor);
    }

    @Test
    public void testAddAndGetSingleSubmission() {
        submissionStore.addSubmission(Submission.builder()
                .setTeamId(TEST_TEAM_ID)
                .setPuzzleId(TEST_PUZZLE_ID)
                .setSubmission("guess1")
                .build());

        List<Submission> submissions = submissionStore.getAllSubmissions();
        assertEquals(1, submissions.size());
        assertEquals(1, submissions.get(0).getSubmissionId().intValue());
        assertEquals(TEST_TEAM_ID, submissions.get(0).getTeamId());
        assertEquals(TEST_PUZZLE_ID, submissions.get(0).getPuzzleId());
        assertEquals("guess1", submissions.get(0).getSubmission());
        assertEquals(SubmissionStatus.getDefault(), submissions.get(0).getStatus());
        assertEquals(clock.instant(), submissions.get(0).getTimestamp());

        Submission submission = submissionStore.getSubmission(1).get();
        assertEquals(1, submission.getSubmissionId().intValue());
        assertEquals(TEST_TEAM_ID, submission.getTeamId());
        assertEquals(TEST_PUZZLE_ID, submission.getPuzzleId());
        assertEquals("guess1", submission.getSubmission());
        assertEquals(SubmissionStatus.getDefault(), submission.getStatus());
        assertEquals(clock.instant(), submission.getTimestamp());

        verifyZeroInteractions(eventProcessor);
    }

    @Test
    public void testAddAndGetMultipleSubmissions() {
        Instant firstInstant = clock.instant();
        submissionStore.addSubmission(Submission.builder()
                .setTeamId(TEST_TEAM_ID)
                .setPuzzleId(TEST_PUZZLE_ID)
                .setSubmission("guess1")
                .build());

        clock.setWrappedClock(Clock.fixed(clock.instant().plus(5, ChronoUnit.MINUTES), clock.getZone()));
        Instant secondInstant = clock.instant();
        submissionStore.addSubmission(Submission.builder()
                .setTeamId(TEST_TEAM_ID)
                .setPuzzleId(TEST_PUZZLE_ID)
                .setSubmission("guess2")
                .build());

        List<Submission> submissions = submissionStore.getAllSubmissions();
        assertEquals(2, submissions.size());
        assertEquals(1, submissions.get(0).getSubmissionId().intValue());
        assertEquals("guess1", submissions.get(0).getSubmission());
        assertEquals(firstInstant, submissions.get(0).getTimestamp());
        assertEquals(2, submissions.get(1).getSubmissionId().intValue());
        assertEquals("guess2", submissions.get(1).getSubmission());
        assertEquals(secondInstant, submissions.get(1).getTimestamp());

        verifyZeroInteractions(eventProcessor);
    }

    @Test
    public void testUpdateSubmissionStatus() {
        submissionStore.addSubmission(Submission.builder()
                .setTeamId(TEST_TEAM_ID)
                .setPuzzleId(TEST_PUZZLE_ID)
                .setSubmission("guess1")
                .build());
        Submission submission = submissionStore.getSubmission(1).get();
        assertEquals(SubmissionStatus.SUBMITTED, submission.getStatus());

        submissionStore.setSubmissionStatus(1, SubmissionStatus.ASSIGNED);
        submission = submissionStore.getSubmission(1).get();
        assertEquals(SubmissionStatus.ASSIGNED, submission.getStatus());

        verifyZeroInteractions(eventProcessor);

        submissionStore.setSubmissionStatus(1, SubmissionStatus.CORRECT);
        submission = submissionStore.getSubmission(1).get();
        assertEquals(SubmissionStatus.CORRECT, submission.getStatus());

        verify(eventProcessor, times(1)).process(any(Event.class));
    }

}
