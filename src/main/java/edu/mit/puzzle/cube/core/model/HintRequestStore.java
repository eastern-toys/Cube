package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

public class HintRequestStore {
    private final ConnectionFactory connectionFactory;
    private final Clock clock;

    public HintRequestStore(
            ConnectionFactory connectionFactory
    ) {
        this.connectionFactory = connectionFactory;
        this.clock = Clock.systemUTC();
    }

    public boolean createHintRequest(HintRequest hintRequest) {
        return DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO hint_requests (puzzleId, teamId, request, timestamp) " +
                        "VALUES (?,?,?,?)",
                Lists.newArrayList(
                        hintRequest.getPuzzleId(),
                        hintRequest.getTeamId(),
                        hintRequest.getRequest(),
                        Timestamp.from(clock.instant()))
        ).isPresent();
    }

    public boolean updateHintRequest(
            int hintRequestId,
            HintRequestStatus status,
            @Nullable String callerUsername,
            @Nullable String response
    ) {
        boolean updated = DatabaseHelper.update(
                connectionFactory,
                "UPDATE hint_requests SET status = ?, callerUsername = ?, response = ? " +
                "WHERE hintRequestId = ? AND (status <> ? OR callerUsername <> ? OR response <> ?)",
                Lists.newArrayList(
                        status.toString(),
                        callerUsername,
                        response,
                        hintRequestId,
                        status.toString(),
                        callerUsername,
                        response
                )
        ) > 0;

        return updated;
    }

    public Optional<HintRequest> getHintRequest(int hintRequestId) {
        List<HintRequest> hintRequests = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM hint_requests WHERE hintRequestId = ?",
                Lists.newArrayList(hintRequestId),
                HintRequest.class
        );

        if (hintRequests.size() == 0) {
            return Optional.empty();
        } else if (hintRequests.size() > 1) {
            throw new RuntimeException("Primary key violation in application layer");
        }

        return Optional.of(hintRequests.get(0));
    }

    public List<HintRequest> getNonTerminalHintRequests() {
        return DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM hint_requests WHERE status <> 'ANSWERED' AND status <> 'REJECTED'",
                ImmutableList.of(),
                HintRequest.class
        );
    }

    public List<HintRequest> getHintRequestsForTeamAndPuzzle(String teamId, String puzzleId) {
        return DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM hint_requests WHERE teamId = ? AND puzzleId = ?",
                ImmutableList.of(teamId, puzzleId),
                HintRequest.class
        );
    }
}
