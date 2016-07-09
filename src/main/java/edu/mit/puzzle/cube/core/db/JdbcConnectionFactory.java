package edu.mit.puzzle.cube.core.db;

import com.google.auto.value.AutoValue;

import org.apache.commons.dbcp.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * An implementation of ConnectionFactory that uses an external database via JDBC.
 */
@AutoValue
public abstract class JdbcConnectionFactory implements ConnectionFactory {
    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setDriverClassName(String driverClassName);
        public abstract Builder setJdbcUrl(String jdbcUrl);
        public abstract Builder setUsername(String username);
        public abstract Builder setPassword(String password);

        protected abstract String getDriverClassName();
        protected abstract String getJdbcUrl();
        protected abstract String getUsername();
        protected abstract String getPassword();

        protected abstract Builder setDataSource(DataSource dataSource);

        abstract JdbcConnectionFactory autoBuild();

        public JdbcConnectionFactory build() {
            try {
                Class.forName(getDriverClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(getDriverClassName());
            dataSource.setUrl(getJdbcUrl());
            dataSource.setUsername(getUsername());
            dataSource.setPassword(getPassword());

            if (getDriverClassName().equals("org.sqlite.JDBC")) {
                dataSource.addConnectionProperty("foreign_keys", "true");
            }

            setDataSource(dataSource);

            return autoBuild();
        }
    }

    public static Builder builder() {
        return new AutoValue_JdbcConnectionFactory.Builder();
    }

    @Override
    public abstract DataSource getDataSource();

    @Override
    public Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    protected abstract String getDriverClassName();
    protected abstract String getJdbcUrl();
    protected abstract String getUsername();
    protected abstract String getPassword();
}
