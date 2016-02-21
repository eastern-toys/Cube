package edu.mit.puzzle.cube.core.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A ConnectionFactory provides a Connection to the SQL database that contains
 * data about the hunt, runs of the hunt, and unlock statuses of teams on each run.
 *
 * The underlying database is assumed to be SQL but not to be any particular flavor of SQL.
 *
 * If implemented by connecting to an in-memory database, the in-memory connection build
 * should also establish the initial tables and some data. An example is in InMemoryConnectionFactory,
 * which uses in-memory SQLite.
 */
public interface ConnectionFactory {

    /**
     * Gets a Connection to the hunt database.
     *
     * @return The Connection to the hunt database
     * @throws SQLException
     */
    Connection getConnection() throws SQLException;

}
