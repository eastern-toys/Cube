package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.events.GenericEventProcessor;
import edu.mit.puzzle.cube.core.events.SubmissionCompleteEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class SubmissionStore {

    private static Logger LOGGER = LogManager.getLogger(SubmissionStore.class);
    private static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    private final ConnectionFactory connectionFactory;
    private final Clock clock;
    private final GenericEventProcessor eventProcessor;

    public SubmissionStore(
            ConnectionFactory connectionFactory,
            GenericEventProcessor eventProcessor
    ) {
        this(connectionFactory, Clock.systemUTC(), eventProcessor);
    }

    public SubmissionStore(
            ConnectionFactory connectionFactory,
            Clock clock,
            GenericEventProcessor eventProcessor
    ) {
        this.connectionFactory = checkNotNull(connectionFactory);
        this.clock = checkNotNull(clock);
        this.eventProcessor = checkNotNull(eventProcessor);
    }

    public boolean addSubmission(
            String teamId,
            String puzzleId,
            String submissionText
    ) {
        return DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO submissions (puzzleId, teamId, submission, timestamp) " +
                        "VALUES (?,?,?,?)",
                Lists.newArrayList(puzzleId, teamId, submissionText, clock.instant())
        ).isPresent();
    }

    private static Submission generateSubmissionObject(Map<String,Object> rowMap) {
        return new Submission(
                (Integer) rowMap.get("submissionId"),
                (String) rowMap.get("teamId"),
                (String) rowMap.get("puzzleId"),
                (String) rowMap.get("submission"),
                SubmissionStatus.valueOf((String) rowMap.get("status")),
                (Instant) rowMap.get("timestamp")
        );
    }

    public List<Submission> getAllSubmissions() {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM submissions",
                Lists.newArrayList(),
                "submissionId"
        );

        List<Submission> submissions = resultTable.rowMap().values().stream()
                .map(SubmissionStore::generateSubmissionObject)
                .collect(Collectors.toList());

        return Ordering.natural().onResultOf(Submission::getSubmissionId).immutableSortedCopy(submissions);
    }

    public Optional<Submission> getSubmission(int submissionId) {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM submissions WHERE submissionId = ?",
                Lists.newArrayList(submissionId),
                "submissionId"
        );

        if (resultTable.rowKeySet().size() == 0) {
            return Optional.empty();
        } else if (resultTable.rowKeySet().size() > 1) {
            throw new RuntimeException("Primary key violation in application layer");
        }

        return Optional.of(generateSubmissionObject(resultTable.row(submissionId)));
    }

    public boolean setSubmissionStatus(int submissionId, SubmissionStatus status) {
        boolean updated = DatabaseHelper.update(
                connectionFactory,
                "UPDATE submissions SET status = ? WHERE submissionId = ? AND status <> ?",
                Lists.newArrayList(status, submissionId, status)
        ) > 0;

        if (updated && status.isTerminal()) {
            Submission submission = this.getSubmission(submissionId).get();
            eventProcessor.process(new SubmissionCompleteEvent(submission));
        }

        return updated;
    }

}
