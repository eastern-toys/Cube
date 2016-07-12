package edu.mit.puzzle.cube.core.model;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.SyncPuzzlesEvent;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class PuzzleStore {
    private ConnectionFactory connectionFactory;
    private HuntDefinition huntDefinition;

    public PuzzleStore(
            ConnectionFactory connectionFactory,
            HuntDefinition huntDefinition,
            CompositeEventProcessor eventProcessor
    ) {
        this.connectionFactory = connectionFactory;
        this.huntDefinition = huntDefinition;

        eventProcessor.addEventProcessor(SyncPuzzlesEvent.class, event -> {
            syncPuzzles();
        });
    }

    /**
     * Copies the list of puzzle ids from the hunt definition to the database.
     */
    public void syncPuzzles() {
        List<String> puzzleIds = huntDefinition.getPuzzleList();
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement insertPuzzleStatement = connection.prepareStatement(
                        "INSERT INTO puzzles (puzzleId) SELECT ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM puzzles WHERE puzzleId = ?)")
        ) {
            connection.setAutoCommit(false);
            for (String puzzleId : puzzleIds) {
                insertPuzzleStatement.setString(1, puzzleId);
                insertPuzzleStatement.setString(2, puzzleId);
                insertPuzzleStatement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.SERVER_ERROR_INTERNAL.getCode(),
                    e,
                    "Failed to sync puzzle list to the database");
        }
    }
}
