package edu.mit.puzzle.cube.core.model;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.VisibilityChangeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class HuntStatusStore {

    private static Logger LOGGER = LogManager.getLogger(HuntStatusStore.class);

    private final ConnectionFactory connectionFactory;
    private final Clock clock;
    private final VisibilityStatusSet visibilityStatusSet;
    private final EventProcessor<Event> eventProcessor;

    public HuntStatusStore(
        ConnectionFactory connectionFactory,
        VisibilityStatusSet visibilityStatusSet,
        EventProcessor<Event> eventProcessor
    ) {
        this(connectionFactory, Clock.systemUTC(), visibilityStatusSet, eventProcessor);
    }

    public HuntStatusStore(
            ConnectionFactory connectionFactory,
            Clock clock,
            VisibilityStatusSet visibilityStatusSet,
            EventProcessor<Event> eventProcessor
    ) {
        this.connectionFactory = checkNotNull(connectionFactory);
        this.clock = checkNotNull(clock);
        this.visibilityStatusSet = checkNotNull(visibilityStatusSet);
        this.eventProcessor = checkNotNull(eventProcessor);
    }

    public VisibilityStatusSet getVisibilityStatusSet() {
        return this.visibilityStatusSet;
    }

    public String getVisibility(String teamId, String puzzleId) {
        return getExplicitVisibility(teamId, puzzleId).orElse(visibilityStatusSet.getDefaultVisibilityStatus());
    }

    public List<Visibility> getExplicitVisibilities(
            Optional<String> teamId,
            Optional<String> puzzleId
    ) {
        String query = "SELECT teamId, puzzleId, status FROM visibilities";
        List<Object> parameters = Lists.newArrayList();
        if (teamId.isPresent() && puzzleId.isPresent()) {
            query += " WHERE teamId = ? AND puzzleId = ?";
            parameters.add(teamId.get());
            parameters.add(puzzleId.get());
        } else if (teamId.isPresent()) {
            query += " WHERE teamId = ?";
            parameters.add(teamId.get());
        } else if (puzzleId.isPresent()) {
            query += " WHERE puzzleId = ?";
            parameters.add(puzzleId.get());
        }

        Table<Integer,String,Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                query,
                parameters
        );

        return resultTable.rowMap().values().stream()
                .map(rowMap ->
                    Visibility.builder()
                            .setTeamId((String) rowMap.get("teamId"))
                            .setPuzzleId((String) rowMap.get("puzzleId"))
                            .setStatus((String) rowMap.get("status"))
                            .build()
                )
                .collect(Collectors.toList());
    }

    public Map<String,String> getVisibilitiesForTeam(String teamId) {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT puzzles.puzzleId AS puzzleId, visibilities.status AS status FROM puzzles " +
                        "LEFT JOIN visibilities ON " +
                            "puzzles.puzzleId = visibilities.puzzleId " +
                            "AND visibilities.teamId = ?",
                Lists.newArrayList(teamId)
        );

        ImmutableMap.Builder<String,String> mapBuilder = ImmutableMap.builder();
        for (Map<String,Object> rowMap : resultTable.rowMap().values()) {
            String puzzleId = (String) rowMap.get("puzzleId");
            String status = Optional.ofNullable((String) rowMap.get("status"))
                    .orElse(visibilityStatusSet.getDefaultVisibilityStatus());
            mapBuilder.put(puzzleId, status);
        }

        return mapBuilder.build();
    }

    public boolean recordHuntRunStart(String runId) {
        Integer updates = DatabaseHelper.update(
                connectionFactory,
                "UPDATE runs SET startTimestamp = ? WHERE runId = ? AND startTimestamp IS NULL",
                Lists.newArrayList(clock.instant(), runId)
        );
        return updates > 0;
    }

    public Map<String,Object> getHuntRunProperties(String runId) {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM runs " +
                        "WHERE runId = ?",
                Lists.newArrayList(runId)
        );

        if (resultTable.rowKeySet().size() == 1) {
            return resultTable.row(0);
        } else {
            return ImmutableMap.of();
        }
    }

    public List<String> getRunIds() {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT runId FROM runs",
                Lists.newArrayList()
        );

        return resultTable.values().stream()
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    public String getRunForTeam(String teamId) {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT runId FROM teams WHERE teamId = ?",
                Lists.newArrayList(teamId)
        );

        return (String) resultTable.get(0, "runId");
    }

    public Set<String> getTeamIds(String runId) {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT teamId FROM teams WHERE runId = ?",
                Lists.newArrayList(runId)
        );

        return resultTable.values().stream().map(o -> (String) o).collect(Collectors.toSet());
    }

    public Map<String,Object> getTeamProperties(String teamId) {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT propertyKey, propertyValue FROM team_properties " +
                        "WHERE teamId = ?",
                Lists.newArrayList(teamId)
        );

        ImmutableMap.Builder<String,Object> mapBuilder = ImmutableMap.builder();
        for (Map<String,Object> rowMap : resultTable.rowMap().values()) {
            String key = (String) rowMap.get("propertyKey");
            Object value = rowMap.get("propertyValue");
            mapBuilder.put(key, value);
        }

        return mapBuilder.build();
    }

    public boolean setTeamProperty(String teamId, String propertyKey, Object propertyValue) {
        Optional<Integer> generatedId = DatabaseHelper.insert(
                connectionFactory,
                "INSERT OR IGNORE INTO team_properties (teamId, propertyKey, propertyValue) VALUES (?,?,?)",
                Lists.newArrayList(teamId, propertyKey, propertyValue));
        if (generatedId.isPresent()) {
            return true;
        }

        int updates = DatabaseHelper.update(
                connectionFactory,
                "UPDATE team_properties SET propertyValue = ? " +
                        "WHERE teamId = ? AND propertyKey = ?",
                Lists.newArrayList(propertyValue, teamId, propertyKey)
        );
        return updates > 0;
    }

    private Optional<String> getExplicitVisibility(String teamId, String puzzleId) {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT status FROM visibilities WHERE teamId = ? AND puzzleId = ?",
                Lists.newArrayList(teamId, puzzleId)
        );
        if (resultTable.rowKeySet().size() == 1) {
            return Optional.of((String) Iterables.getOnlyElement(resultTable.rowMap().values()).get("status"));
        } else if (resultTable.rowKeySet().size() == 0) {
            return Optional.empty();
        } else {
            throw new RuntimeException("Primary key violation in application layer");
        }
    }

    private boolean createExplicitDefaultVisibility(String teamId, String puzzleId) {
        Optional<Integer> generatedId = DatabaseHelper.insert(
                connectionFactory,
                "INSERT OR IGNORE INTO visibilities (teamId, puzzleId) VALUES (?, ?)",
                Lists.newArrayList(teamId, puzzleId));
        return generatedId.isPresent();
    }

    public boolean setVisibility(
            String teamId,
            String puzzleId,
            String status,
            boolean isExternallyInitiated
    ) {
        if (!visibilityStatusSet.isAllowedStatus(status)) {
            return false;
        }
        //Create with default status if necessary first
        createExplicitDefaultVisibility(teamId, puzzleId);

        Set<String> allowedCurrentStatuses = visibilityStatusSet.getAllowedAntecedents(status);
        if (allowedCurrentStatuses.isEmpty()) {
            return false;
        }

        String preparedUpdateSql = "UPDATE visibilities SET status = ? " +
                "WHERE teamId = ? AND puzzleId = ?" + " AND ";
        List<Object> preparedParameters = Lists.newArrayList(status, teamId, puzzleId);
        preparedUpdateSql += "(" +
                Joiner.on(" OR ").join(allowedCurrentStatuses.stream()
                        .map(s -> "status = ?")
                        .collect(Collectors.toList())) +
                ")";
        preparedParameters.addAll(allowedCurrentStatuses);

        int updates = DatabaseHelper.update(
                connectionFactory,
                preparedUpdateSql,
                preparedParameters
        );

        //If we made an update, log the history.
        if (updates > 0) {
            DatabaseHelper.update(
                    connectionFactory,
                    "INSERT INTO visibility_history (teamId, puzzleId, status, timestamp) VALUES (?, ?, ?, ?)",
                    Lists.newArrayList(teamId, puzzleId, status, clock.instant()));

            VisibilityChangeEvent changeEvent = new VisibilityChangeEvent(Visibility.builder()
                    .setTeamId(teamId)
                    .setPuzzleId(puzzleId)
                    .setStatus(status)
                    .build());
            eventProcessor.process(changeEvent);

            return true;
        } else {
            return false;
        }

    }

    public Table<Integer,String,Object> getVisibilityHistory(String teamId, String puzzleId) {
        return DatabaseHelper.query(
                connectionFactory,
                "SELECT status, timestamp FROM visibility_history WHERE " +
                        "teamId = ? AND puzzleId = ? ORDER BY timestamp ASC",
                Lists.newArrayList(teamId, puzzleId)
        );
    }

}
