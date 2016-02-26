package edu.mit.puzzle.cube.core.environments;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;

public interface ServiceEnvironment {

    ConnectionFactory getConnectionFactory();

}
