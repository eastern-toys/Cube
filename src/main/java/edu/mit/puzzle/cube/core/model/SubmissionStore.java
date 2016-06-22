package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.SubmissionCompleteEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class SubmissionStore {

    private static Logger LOGGER = LoggerFactory.getLogger(SubmissionStore.class);

    private final ConnectionFactory connectionFactory;
    private final Clock clock;
    private final EventProcessor<Event> eventProcessor;

    public SubmissionStore(
            ConnectionFactory connectionFactory,
            EventProcessor<Event> eventProcessor
    ) {
        this(connectionFactory, Clock.systemUTC(), eventProcessor);
    }

    public SubmissionStore(
            ConnectionFactory connectionFactory,
            Clock clock,
            EventProcessor<Event> eventProcessor
    ) {
        this.connectionFactory = checkNotNull(connectionFactory);
        this.clock = checkNotNull(clock);
        this.eventProcessor = checkNotNull(eventProcessor);
    }

    public boolean addSubmission(Submission submission) {
        return DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO submissions (puzzleId, teamId, submission, timestamp) " +
                        "VALUES (?,?,?,?)",
                Lists.newArrayList(
                        submission.getPuzzleId(),
                        submission.getTeamId(),
                        submission.getSubmission(),
                        Timestamp.from(clock.instant()))
        ).isPresent();
    }

    public List<Submission> getAllSubmissions() {
        List<Submission> submissions = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM submissions",
                Lists.newArrayList(),
                Submission.class
        );

        return Ordering.natural().onResultOf(Submission::getSubmissionId).immutableSortedCopy(submissions);
    }

    public Optional<Submission> getSubmission(int submissionId) {
        List<Submission> submissions = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM submissions WHERE submissionId = ?",
                Lists.newArrayList(submissionId),
                Submission.class
        );

        if (submissions.size() == 0) {
            return Optional.empty();
        } else if (submissions.size() > 1) {
            throw new RuntimeException("Primary key violation in application layer");
        }

        return Optional.of(submissions.get(0));
    }

    public boolean setSubmissionStatus(
            int submissionId, SubmissionStatus status, @Nullable String callerUsername) {
        boolean updated = DatabaseHelper.update(
                connectionFactory,
                "UPDATE submissions SET status = ?, callerUsername = ? " +
                "WHERE submissionId = ? AND (status <> ? OR callerUsername <> ?)",
                Lists.newArrayList(status, callerUsername, submissionId, status, callerUsername)
        ) > 0;

        if (updated && status.isTerminal()) {
            Submission submission = this.getSubmission(submissionId).get();
            eventProcessor.process(SubmissionCompleteEvent.builder()
                    .setSubmission(submission)
                    .build());
        }

        return updated;
    }

}
