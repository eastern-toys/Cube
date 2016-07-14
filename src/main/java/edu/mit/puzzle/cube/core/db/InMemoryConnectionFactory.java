package edu.mit.puzzle.cube.core.db;

import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.core.model.UserStore;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

/**
 * An implementation of ConnectionFactory that wraps an SQLite in-memory database.
 *
 * This class should not be used in production because you should be able to deploy
 * multiple production service instances, and in-memory databases are local to a single
 * JVM. However, it's still useful for development and particularly for unit testing.
 */
public class InMemoryConnectionFactory implements ConnectionFactory {

    private static class InMemorySQLiteDataSource extends SQLiteDataSource {
        //We hold on to a connection in this class to keep the shared SQLite in-memory
        //database alive. In theory, we could also hold onto this connection and
        //just provide it for every getConnection() call (therefore using a single
        //connection for everything, which is probably okay for the cases where in-memory
        //is useful, but in practice, the fact that Connection implements Autocloseable means
        //that code would constantly be trying to close the Connection and overriding that
        //behavior is a pain.
        @SuppressWarnings("unused")
        private final Connection connection;

        InMemorySQLiteDataSource() {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            setUrl("jdbc:sqlite:file::memory:?cache=shared");

            try {
                connection = getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection newConnection = super.getConnection();
            newConnection.createStatement().executeUpdate("PRAGMA foreign_keys = ON");
            return newConnection;
        }
    }

    protected final DataSource dataSource;

    public InMemoryConnectionFactory(
            VisibilityStatusSet visibilityStatusSet,
            List<String> teamIdList,
            List<String> puzzleIdList,
            List<User> userList
    ) throws SQLException {
        dataSource = new InMemorySQLiteDataSource();

        // Each time we create a new InMemoryConnectionFactory, clear any state from the shared
        // in-memory database that may have been introduced by previous instances.
        clear();

        //Boot up the initial state of tables
        createInitialConfiguration(visibilityStatusSet, teamIdList, puzzleIdList, userList);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    //The initial configuration takes in lists of team ids, puzzle ids and users to preload the
    //database. For a production off-box database, this wouldn't be necessary because you'd
    //just load the data there, but for an in-memory database, we need to set it up in code.
    private void createInitialConfiguration(
            VisibilityStatusSet visibilityStatusSet,
            List<String> teamIdList,
            List<String> puzzleIdList,
            List<User> userList
    ) {
        CubeDatabaseSchema cubeDatabaseSchema = new CubeDatabaseSchema("org.sqlite.JDBC");
        try (Connection connection = getConnection()) {
            cubeDatabaseSchema.execute(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

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

        UserStore userStore = new UserStore(this);
        for (User user : userList) {
            userStore.addUser(user);
        }
    }

    // Clear the state of the shared database. Useful to run between unit tests.
    public void clear() {
        try (Connection connection = getConnection()) {
            connection.createStatement().executeUpdate("PRAGMA writable_schema = 1");
            connection.createStatement().executeUpdate(
                    "delete from sqlite_master where type in ('table', 'index', 'trigger')");
            connection.createStatement().executeUpdate("PRAGMA writable_schema = 0");
            connection.createStatement().executeUpdate("VACUUM");
            connection.createStatement().executeUpdate("PRAGMA INTEGRITY_CHECK");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
