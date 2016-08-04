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
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
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
                Lists.newArrayList(Answer.create(TEST_PUZZLE_ID, "ANSWER")),
                ImmutableList.<User>of(User.builder()
                        .setUsername("writingteamuser")
                        .setPassword("password")
                        .setRoles(ImmutableList.of("writingteam"))
                        .build()));
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

        List<Submission> submissions = submissionStore.getAllSubmissions(
                SubmissionStore.PaginationOptions.none()
        );
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

        List<Submission> submissions = submissionStore.getAllSubmissions(
                SubmissionStore.PaginationOptions.none()
        );
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
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.SUBMITTED
        )).hasSize(1);
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.ASSIGNED
        )).hasSize(0);

        submissionStore.setSubmissionStatus(1, SubmissionStatus.ASSIGNED, "writingteamuser");
        submission = submissionStore.getSubmission(1).get();
        assertEquals(SubmissionStatus.ASSIGNED, submission.getStatus());
        assertEquals("writingteamuser", submission.getCallerUsername());
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.SUBMITTED
        )).hasSize(0);
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.ASSIGNED
        )).hasSize(1);

        verifyZeroInteractions(eventProcessor);

        //Unassign
        submissionStore.setSubmissionStatus(1, SubmissionStatus.SUBMITTED, null);
        submission = submissionStore.getSubmission(1).get();
        assertEquals(SubmissionStatus.SUBMITTED, submission.getStatus());
        assertEquals(null, submission.getCallerUsername());
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.SUBMITTED
        )).hasSize(1);
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.ASSIGNED
        )).hasSize(0);

        verifyZeroInteractions(eventProcessor);

        //Reassign
        submissionStore.setSubmissionStatus(1, SubmissionStatus.ASSIGNED, "writingteamuser");
        submission = submissionStore.getSubmission(1).get();
        assertEquals(SubmissionStatus.ASSIGNED, submission.getStatus());
        assertEquals("writingteamuser", submission.getCallerUsername());
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.SUBMITTED
        )).hasSize(0);
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.ASSIGNED
        )).hasSize(1);

        verifyZeroInteractions(eventProcessor);

        submissionStore.setSubmissionStatus(1, SubmissionStatus.CORRECT, "writingteamuser");
        submission = submissionStore.getSubmission(1).get();
        assertEquals(SubmissionStatus.CORRECT, submission.getStatus());
        assertEquals("writingteamuser", submission.getCallerUsername());
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.ASSIGNED
        )).hasSize(0);
        assertThat(submissionStore.getSubmissionsByStatus(
                SubmissionStore.PaginationOptions.none(),
                SubmissionStatus.CORRECT
        )).hasSize(1);

        verify(eventProcessor, times(1)).process(any(Event.class));
    }

    @Test
    public void testPagination() {
        for (int i = 0; i < 50; i++) {
            submissionStore.addSubmission(Submission.builder()
                    .setTeamId(TEST_TEAM_ID)
                    .setPuzzleId(TEST_PUZZLE_ID)
                    .setSubmission(String.format("guess%d", i))
                    .build());
        }

        List<Submission> submissions = submissionStore.getAllSubmissions(
                SubmissionStore.PaginationOptions.builder()
                        .setPageSize(Optional.of(10))
                        .build()
        );
        assertThat(submissions).hasSize(10);
        assertThat(submissions.get(0).getSubmission()).isEqualTo("guess0");
        assertThat(submissions.get(9).getSubmission()).isEqualTo("guess9");

        submissions = submissionStore.getAllSubmissions(
                SubmissionStore.PaginationOptions.builder()
                        .setStartSubmissionId(Optional.of(submissions.get(9).getSubmissionId()))
                        .setPageSize(Optional.of(10))
                        .build()
        );
        assertThat(submissions).hasSize(10);
        assertThat(submissions.get(0).getSubmission()).isEqualTo("guess10");
        assertThat(submissions.get(9).getSubmission()).isEqualTo("guess19");

        submissions = submissionStore.getSubmissionsByTeam(
                SubmissionStore.PaginationOptions.builder()
                        .setStartSubmissionId(Optional.of(submissions.get(9).getSubmissionId()))
                        .setPageSize(Optional.of(10))
                        .build(),
                TEST_TEAM_ID
        );
        assertThat(submissions).hasSize(10);
        assertThat(submissions.get(0).getSubmission()).isEqualTo("guess20");
        assertThat(submissions.get(9).getSubmission()).isEqualTo("guess29");

        submissions = submissionStore.getSubmissionsByTeamAndPuzzle(
                SubmissionStore.PaginationOptions.builder()
                        .setStartSubmissionId(Optional.of(submissions.get(9).getSubmissionId()))
                        .setPageSize(Optional.of(10))
                        .build(),
                TEST_TEAM_ID,
                TEST_PUZZLE_ID
        );
        assertThat(submissions).hasSize(10);
        assertThat(submissions.get(0).getSubmission()).isEqualTo("guess30");
        assertThat(submissions.get(9).getSubmission()).isEqualTo("guess39");

        submissions = submissionStore.getAllSubmissions(
                SubmissionStore.PaginationOptions.builder()
                        .setStartSubmissionId(Optional.of(50))
                        .setPageSize(Optional.of(10))
                        .build()
        );
        assertThat(submissions).isEmpty();
    }
}
