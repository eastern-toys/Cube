package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.VisibilityChangeEvent;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class HuntStatusStore {

    private static Logger LOGGER = LoggerFactory.getLogger(HuntStatusStore.class);

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

    public boolean recordHuntRunStart() {
        Integer updates = DatabaseHelper.update(
                connectionFactory,
                "UPDATE run SET startTimestamp = ? WHERE startTimestamp IS NULL",
                Lists.newArrayList(clock.instant())
        );
        return updates > 0;
    }

    public Map<String,Object> getHuntRunProperties() {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM run",
                Lists.newArrayList()
        );

        if (resultTable.rowKeySet().size() == 1) {
            return resultTable.row(0);
        } else {
            return ImmutableMap.of();
        }
    }

    public Set<String> getTeamIds() {
        Table<Integer, String, Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT teamId FROM teams",
                Lists.newArrayList()
        );

        return resultTable.values().stream().map(o -> (String) o).collect(Collectors.toSet());
    }

    public Team getTeam(String teamId) {
        Table<Integer, String, Object> teamPropertiesResults = DatabaseHelper.query(
                connectionFactory,
                "SELECT propertyKey, propertyValue FROM team_properties " +
                        "WHERE teamId = ?",
                Lists.newArrayList(teamId)
        );

        ImmutableMap.Builder<String, Team.Property> teamProperties = ImmutableMap.builder();
        for (Map<String,Object> rowMap : teamPropertiesResults.rowMap().values()) {
            String key = (String) rowMap.get("propertyKey");
            String value = (String) rowMap.get("propertyValue");
            Class<? extends Team.Property> propertyClass = Team.Property.getClass(key);
            try {
                Team.Property property = new ObjectMapper().readValue(value, propertyClass);
                teamProperties.put(key, property);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return Team.builder()
                .setTeamId(teamId)
                .setTeamProperties(teamProperties.build())
                .build();
    }

    public boolean setTeamProperty(
            String teamId,
            Class<? extends Team.Property> propertyClass,
            Team.Property property) {
        String propertyKey = propertyClass.getSimpleName();
        Preconditions.checkArgument(
                propertyClass.isInstance(property),
                "Team property is not an instance of %s",
                propertyKey);
        String propertyValue;
        try {
            propertyValue = new ObjectMapper().writeValueAsString(property);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

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

    public void addTeam(Team team) {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement insertTeamStatement = connection.prepareStatement(
                        "INSERT INTO teams (teamId) VALUES (?)")
        ) {
            insertTeamStatement.setString(1, team.getTeamId());
            insertTeamStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    e,
                    "Failed to add team to the database");
        }
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

            VisibilityChangeEvent changeEvent = VisibilityChangeEvent.builder()
                    .setVisibility(Visibility.builder()
                            .setTeamId(teamId)
                            .setPuzzleId(puzzleId)
                            .setStatus(status)
                            .build())
                    .build();
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

    // TODO: introduce some filtering and/or pagination on this API - always reading all
    // visibility changes may not scale.
    public List<VisibilityChange> getVisibilityChanges() {
        Table<Integer,String,Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT teamId, puzzleId, status, timestamp FROM visibility_history",
                ImmutableList.<Object>of());
        ImmutableList.Builder<VisibilityChange> visibilityChanges = ImmutableList.builder();
        for (Map<String,Object> rowMap : resultTable.rowMap().values()) {
            visibilityChanges.add(VisibilityChange.builder()
                    .setTeamId((String) rowMap.get("teamId"))
                    .setPuzzleId((String) rowMap.get("puzzleId"))
                    .setStatus((String) rowMap.get("status"))
                    .setTimestamp((Instant) rowMap.get("timestamp"))
                    .build());
        }
        return visibilityChanges.build();
    }
}
