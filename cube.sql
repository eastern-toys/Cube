CREATE TABLE run (
       startTimestamp TIMESTAMP DEFAULT NULL
);
INSERT INTO run (startTimestamp) VALUES (NULL);

CREATE TABLE teams (
       teamId VARCHAR(20),
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
INSERT INTO users (username, password, password_salt) VALUES(
       'admin',
       '876831c0f3d4154828b60527f9c662ab3634b302d7a138a2d1f1eda757780acf41867a156fe67ddd49d8245fe902c2e7b92d7c987bb931000d08a6796d86aff9',  -- password is 'admin'
       '4OyUlr32y9YOAuUSOQ9L5A=='
);

CREATE TABLE roles (
       role_name VARCHAR(40),
       PRIMARY KEY(role_name)
);
INSERT INTO roles (role_name) VALUES ('admin'), ('writingteam');

CREATE TABLE roles_permissions (
       role_name VARCHAR(40),
       permission VARCHAR(40),
       FOREIGN KEY(role_name) REFERENCES roles(role_name)
);
INSERT INTO roles_permissions (role_name, permission) VALUES
       ('admin', '*'),
       ('writingteam', 'users:read:*'),
       ('writingteam', 'userroles:read:*'),
       ('writingteam', 'teams:read:*'),
       ('writingteam', 'submissions:read,update:*'),
       ('writingteam', 'visibilities:read:*')
;

CREATE TABLE user_roles (
       username VARCHAR(40),
       role_name VARCHAR(40),
       FOREIGN KEY(username) REFERENCES users(username),
       FOREIGN KEY(role_name) REFERENCES roles(role_name)
);
INSERT INTO user_roles (username, role_name) VALUES ('admin', 'admin');

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
       PRIMARY KEY(submissionId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId),
       FOREIGN KEY(callerUsername) REFERENCES users(username)
);

CREATE TABLE visibilities (
       teamId VARCHAR(20),
       puzzleId VARCHAR(40),
       status VARCHAR(10) DEFAULT 'INVISIBLE',
       PRIMARY KEY(teamId, puzzleId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId)
);

CREATE TABLE visibility_history (
       visibilityHistoryId ${auto_increment_type},
       teamId VARCHAR(20),
       puzzleId VARCHAR(40),
       status VARCHAR(10) DEFAULT 'INVISIBLE',
       timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY(visibilityHistoryId),
       FOREIGN KEY(teamId) REFERENCES teams(teamId),
       FOREIGN KEY(puzzleId) REFERENCES puzzles(puzzleId)       
);
