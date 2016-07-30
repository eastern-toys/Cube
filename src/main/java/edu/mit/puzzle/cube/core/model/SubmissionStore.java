package edu.mit.puzzle.cube.core.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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

    @AutoValue
    public static abstract class PaginationOptions {
        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder setStartSubmissionId(Integer startSubmissionId);
            public abstract Builder setPageSize(Integer pageSize);

            public abstract PaginationOptions build();
        }

        public static Builder builder() {
            return new AutoValue_SubmissionStore_PaginationOptions.Builder();
        }

        public static PaginationOptions none() {
            return builder().build();
        }

        @Nullable public abstract Integer getStartSubmissionId();
        @Nullable public abstract Integer getPageSize();

        public String buildSelectQuery(List<String> parameterColumnNames) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM submissions");
            if (parameterColumnNames.size() > 0 || getStartSubmissionId() != null) {
                query.append(" WHERE");
                boolean firstClause = true;
                for (String column : parameterColumnNames) {
                    if (!firstClause) {
                        query.append(" AND");
                    }
                    query.append(String.format(" %s = ?", column));
                    firstClause = false;
                }
                if (getStartSubmissionId() != null) {
                    if (!firstClause) {
                        query.append(" AND");
                    }
                    query.append(String.format(" submissionId > %d", getStartSubmissionId()));
                    firstClause = false;
                }
            }
            query.append(" ORDER BY submissionId");
            if (getPageSize() != null) {
                query.append(String.format(" LIMIT %d", getPageSize()));
            }
            return query.toString();
        }
    }

    public List<Submission> getAllSubmissions(PaginationOptions paginationOptions) {
        return DatabaseHelper.query(
                connectionFactory,
                paginationOptions.buildSelectQuery(ImmutableList.of()),
                Lists.newArrayList(),
                Submission.class
        );
    }

    public List<Submission> getSubmissionsByStatus(
            PaginationOptions paginationOptions,
            SubmissionStatus status
    ) {
        return DatabaseHelper.query(
                connectionFactory,
                paginationOptions.buildSelectQuery(ImmutableList.of("status")),
                Lists.newArrayList(status),
                Submission.class
        );
    }

    public List<Submission> getSubmissionsByTeam(
            PaginationOptions paginationOptions,
            String teamId
    ) {
        return DatabaseHelper.query(
                connectionFactory,
                paginationOptions.buildSelectQuery(ImmutableList.of("teamId")),
                Lists.newArrayList(teamId),
                Submission.class
        );
    }

    public List<Submission> getSubmissionsByTeamAndPuzzle(
            PaginationOptions paginationOptions,
            String teamId,
            String puzzleId
    ) {
        return DatabaseHelper.query(
                connectionFactory,
                paginationOptions.buildSelectQuery(ImmutableList.of("teamId", "puzzleId")),
                Lists.newArrayList(teamId, puzzleId),
                Submission.class
        );
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
                Lists.newArrayList(status.toString(), callerUsername, submissionId, status.toString(), callerUsername)
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
