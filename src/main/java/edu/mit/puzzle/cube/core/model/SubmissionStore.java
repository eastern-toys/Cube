package edu.mit.puzzle.cube.core.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
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
import java.util.ArrayList;
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
    public static abstract class FilterOptions {
        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder setTeamId(Optional<String> teamId);
            public abstract Builder setPuzzleId(Optional<String> puzzleId);
            public abstract Builder setStatuses(List<SubmissionStatus> statuses);
            public abstract Builder setCallerUsername(Optional<String> callerUsername);

            public abstract FilterOptions build();
        }

        public static Builder builder() {
            return new AutoValue_SubmissionStore_FilterOptions.Builder()
                    .setTeamId(Optional.empty())
                    .setPuzzleId(Optional.empty())
                    .setStatuses(ImmutableList.<SubmissionStatus>of())
                    .setCallerUsername(Optional.empty());
        }

        public abstract Optional<String> getTeamId();
        public abstract Optional<String> getPuzzleId();
        public abstract List<SubmissionStatus> getStatuses();
        public abstract Optional<String> getCallerUsername();

        public boolean hasFilters() {
            return getTeamId().isPresent()
                    || getPuzzleId().isPresent()
                    || getStatuses().size() > 0
                    || getCallerUsername().isPresent();
        }
    }

    @AutoValue
    public static abstract class PaginationOptions {
        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder setStartSubmissionId(Optional<Integer> startSubmissionId);
            public abstract Builder setPageSize(Optional<Integer> pageSize);

            public abstract PaginationOptions build();
        }

        public static Builder builder() {
            return new AutoValue_SubmissionStore_PaginationOptions.Builder()
                    .setStartSubmissionId(Optional.empty())
                    .setPageSize(Optional.empty());
        }

        public static PaginationOptions none() {
            return builder().build();
        }

        public abstract Optional<Integer> getStartSubmissionId();
        public abstract Optional<Integer> getPageSize();
    }

    public List<Submission> getSubmissions(
            FilterOptions filterOptions,
            PaginationOptions paginationOptions
    ) {
        StringBuilder query = new StringBuilder();
        ImmutableList.Builder<Object> parameterList = ImmutableList.builder();
        query.append("SELECT * FROM submissions");
        if (filterOptions.hasFilters() || paginationOptions.getStartSubmissionId().isPresent()) {
            List<String> whereClauses = new ArrayList<>();
            if (filterOptions.getTeamId().isPresent()) {
                whereClauses.add("teamId = ?");
                parameterList.add(filterOptions.getTeamId().get());
            }
            if (filterOptions.getPuzzleId().isPresent()) {
                whereClauses.add("puzzleId = ?");
                parameterList.add(filterOptions.getPuzzleId().get());
            }
            if (!filterOptions.getStatuses().isEmpty()) {
                List<String> statusClauses = new ArrayList<>();
                for (SubmissionStatus status : filterOptions.getStatuses()) {
                    statusClauses.add("status = ?");
                    parameterList.add(status);
                }
                whereClauses.add(String.format("(%s)", Joiner.on(" OR ").join(statusClauses)));
            }
            if (filterOptions.getCallerUsername().isPresent()) {
                whereClauses.add("callerUsername = ?");
                parameterList.add(filterOptions.getCallerUsername().get());
            }
            if (paginationOptions.getStartSubmissionId().isPresent()) {
                whereClauses.add(String.format(
                        " submissionId > %d",
                        paginationOptions.getStartSubmissionId().get()
                ));
            }
            query.append(String.format(" WHERE %s", Joiner.on(" AND ").join(whereClauses)));
        }
        query.append(" ORDER BY submissionId");
        if (paginationOptions.getPageSize().isPresent()) {
            query.append(String.format(" LIMIT %d", paginationOptions.getPageSize().get()));
        }
        return DatabaseHelper.query(
                connectionFactory,
                query.toString(),
                parameterList.build(),
                Submission.class
        );
    }

    public List<Submission> getAllSubmissions(PaginationOptions paginationOptions) {
        return getSubmissions(FilterOptions.builder().build(), paginationOptions);
    }

    public List<Submission> getSubmissionsByStatus(
            PaginationOptions paginationOptions,
            SubmissionStatus status
    ) {
        FilterOptions filterOptions = FilterOptions.builder()
                .setStatuses(ImmutableList.of(status))
                .build();
        return getSubmissions(filterOptions, paginationOptions);
    }

    public List<Submission> getSubmissionsByTeam(
            PaginationOptions paginationOptions,
            String teamId
    ) {
        FilterOptions filterOptions = FilterOptions.builder()
                .setTeamId(Optional.of(teamId))
                .build();
        return getSubmissions(filterOptions, paginationOptions);
    }

    public List<Submission> getSubmissionsByTeamAndPuzzle(
            PaginationOptions paginationOptions,
            String teamId,
            String puzzleId
    ) {
        FilterOptions filterOptions = FilterOptions.builder()
                .setTeamId(Optional.of(teamId))
                .setPuzzleId(Optional.of(puzzleId))
                .build();
        return getSubmissions(filterOptions, paginationOptions);
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
