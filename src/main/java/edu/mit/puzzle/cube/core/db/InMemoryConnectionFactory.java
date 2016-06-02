package edu.mit.puzzle.cube.core.db;

import com.google.common.collect.Lists;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An implementation of ConnectionFactory that wraps an SQLite in-memory database.
 *
 * This class should not be used in production because you should be able to deploy
 * multiple production service instances, and in-memory databases are local to a single
 * JVM. However, it's still useful for development and particularly for unit testing.
 */
public class InMemoryConnectionFactory implements ConnectionFactory {

    //We hold on to a connection in this class so that the JVM garbage collector
    //doesn't harvest it. In theory, we could also hold onto this connection and
    //just provide it for every getConnection() call (therefore using a single
    //connection for everything, which is probably okay for the cases where in-memory
    //is useful, but in practice, the fact that Connection implements Autocloseable means
    //that code would constantly be trying to close the Connection and overriding that
    //behavior is a pain.
    protected Connection connection;

    public InMemoryConnectionFactory(
            VisibilityStatusSet visibilityStatusSet,
            List<String> teamIdList,
            List<String> puzzleIdList
    ) throws SQLException {
        //Store the garbage collection preventing connection
        this.connection = createDefaultInMemoryConnection();
        //Boot up the initial state of tables
        createInitialConfiguration(visibilityStatusSet, teamIdList, puzzleIdList);
    }

    //Getting a connection just creates a new one
    public Connection getConnection() throws SQLException {
        return createDefaultInMemoryConnection();
    }


    protected Connection createDefaultInMemoryConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            //The "?cache=shared" parameter is what allows the in-memory database to be shared
            //across connections. By default, SQLite in-memory databases work with one Connection
            //and are collected when that Connection is closed.
            Connection connection = DriverManager.getConnection("jdbc:sqlite:file::memory:?cache=shared");

            return connection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    //The initial configuration takes in a list of team ids and puzzle ids to preload the
    //database. For a production off-box database, this wouldn't be necessary because you'd
    //just load the data there, but for an in-memory database, we need to set it up in code.
    private void createInitialConfiguration(
            VisibilityStatusSet visibilityStatusSet,
            List<String> teamIdList,
            List<String> puzzleIdList
    ) {
        DatabaseHelper.update(
                this,
                "PRAGMA foreign_keys = ON",
                Lists.newArrayList()
        );

        String createRunTableSql = "CREATE TABLE IF NOT EXISTS run " +
                "(startTimestamp DATETIME DEFAULT NULL)";
        String createTeamsTableSql = "CREATE TABLE IF NOT EXISTS teams " +
                "(teamId VARCHAR(20), " +
                "PRIMARY KEY(teamId ASC))";
        String createTeamPropertiesTableSql = "CREATE TABLE IF NOT EXISTS team_properties " +
                "(teamId VARCHAR(20), " +
                "propertyKey VARCHAR(20), " +
                "propertyValue BLOB, " +
                "PRIMARY KEY(teamId, propertyKey), " +
                "FOREIGN KEY(teamId) REFERENCES teams(teamId))";
        String createPuzzlesTableSql = "CREATE TABLE IF NOT EXISTS puzzles " +
                "(puzzleId VARCHAR(40), " +
                "PRIMARY KEY(puzzleId ASC))";
        String createSubmissionsTableSql = "CREATE TABLE IF NOT EXISTS submissions " +
                "(submissionId INTEGER, puzzleId VARCHAR(40), teamId VARCHAR(20), submission TEXT, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "status VARCHAR(10) DEFAULT '" + SubmissionStatus.getDefault() + "', " +
                "PRIMARY KEY(submissionId ASC), " +
                "FOREIGN KEY(teamId) REFERENCES teams(teamId), " +
                "FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId))";
        String createVisibilitiesTableSql = "CREATE TABLE IF NOT EXISTS visibilities " +
                "(teamId VARCHAR(20), puzzleId VARCHAR(40), " +
                "status VARCHAR(10) DEFAULT '" + visibilityStatusSet.getDefaultVisibilityStatus() + "', " +
                "PRIMARY KEY(teamId, puzzleId), " +
                "FOREIGN KEY(teamId) REFERENCES teams(teamId), " +
                "FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId))";
        String createVisibilityHistoriesTableSql = "CREATE TABLE IF NOT EXISTS visibility_history " +
                "(visibilityHistoryId INTEGER, teamId VARCHAR(20), puzzleId VARCHAR(40), " +
                "status VARCHAR(10) DEFAULT '" + visibilityStatusSet.getDefaultVisibilityStatus() + "', " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY(visibilityHistoryId ASC), " +
                "FOREIGN KEY(teamId) REFERENCES teams(teamId), " +
                "FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId))";

        List<String> createTableSqls = Lists.newArrayList(
                createRunTableSql,
                createTeamsTableSql, createTeamPropertiesTableSql, createPuzzlesTableSql,
                createSubmissionsTableSql, createVisibilitiesTableSql, createVisibilityHistoriesTableSql);
        for (String createTableSql : createTableSqls) {
            DatabaseHelper.update(
                    this,
                    createTableSql,
                    Lists.newArrayList()
            );
        }

        String insertRunSql = "INSERT INTO run (startTimestamp) VALUES (NULL)";
        DatabaseHelper.insert(this, insertRunSql, Lists.newArrayList());

        String insertTeamSql = "INSERT INTO teams (teamId) VALUES (?)";
        List<List<Object>> parameterLists = teamIdList.stream()
                .map(id -> Lists.<Object>newArrayList(id))
                .collect(Collectors.toList());
        DatabaseHelper.insertBatch(this, insertTeamSql, parameterLists);

        String insertPuzzleSql = "INSERT INTO puzzles (puzzleId) VALUES (?)";
        parameterLists = puzzleIdList.stream()
                .map(id -> Lists.<Object>newArrayList(id))
                .collect(Collectors.toList());
        DatabaseHelper.insertBatch(this, insertPuzzleSql, parameterLists);
    }

}
