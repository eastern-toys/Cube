package edu.mit.puzzle.cube.core.db;

import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class InMemorySingleUnsharedConnectionFactory extends InMemoryConnectionFactory {

    public InMemorySingleUnsharedConnectionFactory(
            VisibilityStatusSet visibilityStatusSet,
            List<String> teamIdList,
            List<String> puzzleIdList
    ) throws SQLException {
        super(visibilityStatusSet, teamIdList, puzzleIdList);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    protected Connection createDefaultInMemoryConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = new ForceCloseOnlyConnection(DriverManager.getConnection("jdbc:sqlite:file::memory:"));

            connection.createStatement().executeUpdate("PRAGMA foreign_keys = ON");

            return connection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ForceCloseOnlyConnection implements Connection {

        private Connection wrappedConnection;

        public ForceCloseOnlyConnection(Connection connection) {
            this.wrappedConnection = connection;
        }

        @Override
        public void close() throws SQLException {
            //Don't actually close
        }

        public void forceClose() throws SQLException {
            wrappedConnection.close();
        }

        @Override
        public Statement createStatement() throws SQLException {
            return wrappedConnection.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return wrappedConnection.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return wrappedConnection.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return wrappedConnection.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            wrappedConnection.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return wrappedConnection.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            wrappedConnection.commit();
        }

        @Override
        public void rollback() throws SQLException {
            wrappedConnection.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return wrappedConnection.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return wrappedConnection.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            wrappedConnection.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return wrappedConnection.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            wrappedConnection.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return wrappedConnection.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            wrappedConnection.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return wrappedConnection.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return wrappedConnection.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            wrappedConnection.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return wrappedConnection.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return wrappedConnection.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            wrappedConnection.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            wrappedConnection.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return wrappedConnection.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return wrappedConnection.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return wrappedConnection.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            wrappedConnection.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            wrappedConnection.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return wrappedConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return wrappedConnection.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return wrappedConnection.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return wrappedConnection.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return wrappedConnection.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return wrappedConnection.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return wrappedConnection.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return wrappedConnection.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return wrappedConnection.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            wrappedConnection.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            wrappedConnection.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return wrappedConnection.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return wrappedConnection.getClientInfo();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return wrappedConnection.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return wrappedConnection.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            wrappedConnection.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return wrappedConnection.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            wrappedConnection.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            wrappedConnection.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return wrappedConnection.getNetworkTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return wrappedConnection.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return wrappedConnection.isWrapperFor(iface);
        }
    }
}
