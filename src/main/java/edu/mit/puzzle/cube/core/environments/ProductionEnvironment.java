package edu.mit.puzzle.cube.core.environments;

import com.google.common.base.Preconditions;

import edu.mit.puzzle.cube.core.CubeConfig;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.JdbcConnectionFactory;

public class ProductionEnvironment implements ServiceEnvironment {
    private final ConnectionFactory connectionFactory;

    public ProductionEnvironment(CubeConfig config) {
        Preconditions.checkNotNull(
                config.getDatabaseConfig(),
                "A database config must be provided for the production environment"
        );
        connectionFactory = JdbcConnectionFactory.builder()
                .setDriverClassName(config.getDatabaseConfig().getDriverClassName())
                .setJdbcUrl(config.getDatabaseConfig().getJdbcUrl())
                .setUsername(config.getDatabaseConfig().getUsername())
                .setPassword(config.getDatabaseConfig().getPassword())
                .build();
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}
