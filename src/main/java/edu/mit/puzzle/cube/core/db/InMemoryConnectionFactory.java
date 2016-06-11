package edu.mit.puzzle.cube.core.db;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.model.SubmissionStatus;
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

    // We use this field to detect when a new InMemoryConnectionFactory is created. When this
    // happens, we destroy the data persisted by the old InMemoryConnectionFactory.
    protected static InMemoryConnectionFactory existingFactory = null;

    protected final DataSource dataSource;

    public InMemoryConnectionFactory(
            VisibilityStatusSet visibilityStatusSet,
            List<String> teamIdList,
            List<String> puzzleIdList
    ) throws SQLException {
        if (existingFactory != null) {
            existingFactory.destroy();
        }
        existingFactory = this;

        dataSource = new InMemorySQLiteDataSource();

        //Boot up the initial state of tables
        createInitialConfiguration(visibilityStatusSet, teamIdList, puzzleIdList);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    //The initial configuration takes in a list of team ids and puzzle ids to preload the
    //database. For a production off-box database, this wouldn't be necessary because you'd
    //just load the data there, but for an in-memory database, we need to set it up in code.
    private void createInitialConfiguration(
            VisibilityStatusSet visibilityStatusSet,
            List<String> teamIdList,
            List<String> puzzleIdList
    ) {
        String createRunTableSql = "CREATE TABLE IF NOT EXISTS run " +
                "(startTimestamp DATETIME DEFAULT NULL)";
        String createTeamsTableSql = "CREATE TABLE IF NOT EXISTS teams " +
                "(teamId VARCHAR(20), " +
                "PRIMARY KEY(teamId ASC))";
        String createTeamPropertiesTableSql = "CREATE TABLE IF NOT EXISTS team_properties " +
                "(teamId VARCHAR(20), " +
                "propertyKey VARCHAR(20), " +
                "propertyValue TEXT, " +
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
        String createRolesTableSql = "CREATE TABLE IF NOT EXISTS roles " +
                "(role_name VARCHAR(40), PRIMARY KEY(role_name))";
        String createRolesPermissionsTableSql = "CREATE TABLE IF NOT EXISTS roles_permissions " +
                "(role_name VARCHAR(40), permission VARCHAR(40), " +
                "PRIMARY KEY(role_name, permission), " +
                "FOREIGN KEY(role_name) REFERENCES roles(role_name))";
        String createUsersTableSql = "CREATE TABLE IF NOT EXISTS users " +
                "(username VARCHAR(40), password VARCHAR(40), password_salt VARCHAR(40), " +
                "PRIMARY KEY(username))";
        String createUserRolesTableSql = "CREATE TABLE IF NOT EXISTS user_roles " +
                "(username VARCHAR(40), role_name VARCHAR(40), " +
                "FOREIGN KEY(username) REFERENCES users(username), " +
                "FOREIGN KEY(role_name) REFERENCES roles(role_name))";

        List<String> createTableSqls = Lists.newArrayList(
                createRunTableSql,
                createTeamsTableSql, createTeamPropertiesTableSql, createPuzzlesTableSql,
                createSubmissionsTableSql, createVisibilitiesTableSql, createVisibilityHistoriesTableSql,
                createRolesTableSql, createRolesPermissionsTableSql, createUsersTableSql, createUserRolesTableSql);
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

        String insertRolesSql =
                "INSERT INTO roles (role_name) VALUES (?)";
        DatabaseHelper.insertBatch(this, insertRolesSql, ImmutableList.of(
                ImmutableList.of("admin"),
                ImmutableList.of("writingteam")
        ));

        String insertRolesPermissionsSql =
                "INSERT INTO roles_permissions (role_name, permission) VALUES (?,?)";
        DatabaseHelper.insertBatch(this, insertRolesPermissionsSql, ImmutableList.of(
                ImmutableList.of("admin", "*"),
                ImmutableList.of("writingteam", "submissions:*"),
                ImmutableList.of("writingteam", "visibilities:*")
        ));

        UserStore userStore = new UserStore(this);
        userStore.addUser(
                User.builder().setUsername("adminuser").build(),
                "adminpassword",
                ImmutableList.of("admin"));
        userStore.addUser(
                User.builder().setUsername("writingteamuser").build(),
                "writingteampassword",
                ImmutableList.of("writingteam"));
    }

    // Clear the state of the shared database. Useful to run between unit tests.
    public void destroy() {
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
