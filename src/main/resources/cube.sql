CREATE TABLE run (
       startTimestamp TIMESTAMP DEFAULT NULL
);
INSERT INTO run (startTimestamp) VALUES (NULL);

CREATE TABLE teams (
       teamId VARCHAR(20),
       email VARCHAR(320),
       primaryPhone VARCHAR(30),
       secondaryPhone VARCHAR(30),
       PRIMARY KEY(teamId)
);

CREATE TABLE team_properties (
       teamId VARCHAR(20),
       propertyKey VARCHAR(20),
       propertyValue TEXT,
       PRIMARY KEY(teamId, propertyKey),
       FOREIGN KEY(teamId) REFERENCES teams(teamId)
);

CREATE TABLE users (
       username VARCHAR(40),
       password VARCHAR(128),
       password_salt VARCHAR(24),
       teamId VARCHAR(20),
       PRIMARY KEY(username),
       FOREIGN KEY(teamId) REFERENCES teams(teamId)
);

CREATE TABLE roles (
       role_name VARCHAR(40),
       PRIMARY KEY(role_name)
);

CREATE TABLE roles_permissions (
       role_name VARCHAR(40),
       permission VARCHAR(40),
       FOREIGN KEY(role_name) REFERENCES roles(role_name)
);

CREATE TABLE user_roles (
       username VARCHAR(40),
       role_name VARCHAR(40),
       FOREIGN KEY(username) REFERENCES users(username),
       FOREIGN KEY(role_name) REFERENCES roles(role_name)
);

CREATE TABLE users_permissions (
       username VARCHAR(40),
       permission VARCHAR(40),
       FOREIGN KEY(username) REFERENCES users(username)
);

CREATE TABLE puzzles (
       puzzleId VARCHAR(40),
       PRIMARY KEY(puzzleId)
);

CREATE TABLE submissions (
       submissionId ${auto_increment_type},
       teamId VARCHAR(20),
       puzzleId VARCHAR(40),
       submission TEXT,
       timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       status VARCHAR(10) DEFAULT 'SUBMITTED',
       callerUsername VARCHAR(40),
       canonicalAnswer TEXT,
       PRIMARY KEY(submissionId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId),
       FOREIGN KEY(callerUsername) REFERENCES users(username)
);

CREATE TABLE visibilities (
       teamId VARCHAR(20),
       puzzleId VARCHAR(40),
       status VARCHAR(10) DEFAULT '${default_visibility_status}',
       PRIMARY KEY(teamId, puzzleId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId)
);

CREATE TABLE visibility_history (
       visibilityHistoryId ${auto_increment_type},
       teamId VARCHAR(20),
       puzzleId VARCHAR(40),
       status VARCHAR(10) DEFAULT '${default_visibility_status}',
       timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY(visibilityHistoryId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId)       
);
