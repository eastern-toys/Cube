package edu.mit.puzzle.cube.tool;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableMap;

import edu.mit.puzzle.cube.core.CubeConfig;
import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.db.CubeDatabaseSchema;
import edu.mit.puzzle.cube.core.environments.ProductionEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.model.Answer;
import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.core.model.UserStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class CubeTool {
    private final JCommander jCommander;
    private final CubeConfig cubeConfig;
    private final ServiceEnvironment environment;

    private CubeTool() {
        jCommander = new JCommander(this);
        jCommander.setProgramName(CubeTool.class.getSimpleName());

        cubeConfig = CubeConfig.readFromConfigJson();
        environment = new ProductionEnvironment(cubeConfig);
    }

    public static class NonEmptyStringValidator implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            if (value.trim().isEmpty()) {
                throw new ParameterException("Parameter " + name + " must not be empty");
            }
        }
    }

    private interface Command {
        public void run();
    }

    @Parameters(
            commandNames = {"initdb"},
            commandDescription = "Initialize an empty database for use with Cube"
    )
    private class CommandInitDb implements Command {
        @Override
        public void run() {
            HuntDefinition huntDefinition = HuntDefinition.forClassName(
                    cubeConfig.getHuntDefinitionClassName()
            );
            CubeDatabaseSchema cubeDatabaseSchema = new CubeDatabaseSchema(
                    cubeConfig.getDatabaseConfig().getDriverClassName(),
                    huntDefinition.getVisibilityStatusSet()
            );
            try (
                    Connection connection = environment.getConnectionFactory().getConnection()
            ) {
                cubeDatabaseSchema.execute(connection);
                try (
                        PreparedStatement insertPuzzleStatement = connection.prepareStatement(
                                "INSERT INTO puzzles (puzzleId) VALUES (?)")
                ) {
                    for (Answer answer : huntDefinition.getPuzzleList()) {
                        insertPuzzleStatement.setString(1, answer.getPuzzleId());
                        insertPuzzleStatement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Parameters(
            commandNames = {"resethunt"},
            commandDescription = "Delete all run progress data from the database"
    )
    private class CommandResetHunt implements Command {
        @Override
        public void run() {
            try (
                    Connection connection = environment.getConnectionFactory().getConnection();
                    PreparedStatement updateRun = connection.prepareStatement(
                            "UPDATE run SET startTimestamp = NULL");
                    PreparedStatement deleteTeamProperties = connection.prepareStatement(
                            "DELETE FROM team_properties");
                    PreparedStatement deleteSubmissions = connection.prepareStatement(
                            "DELETE FROM submissions");
                    PreparedStatement deleteVisibilities = connection.prepareStatement(
                            "DELETE FROM visibilities");
                    PreparedStatement deleteVisibilityHistory = connection.prepareStatement(
                            "DELETE FROM visibility_history");
            ) {
                updateRun.executeUpdate();
                deleteTeamProperties.executeUpdate();
                deleteSubmissions.executeUpdate();
                deleteVisibilities.executeUpdate();
                deleteVisibilityHistory.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Parameters(
            commandNames = {"adduser"},
            commandDescription = "Create a new user"
    )
    private class CommandAddUser implements Command {
        @Parameter(
                names = {"-u", "--username"},
                description = "The username of the user to create",
                required = true,
                validateWith = NonEmptyStringValidator.class
        )
        String username;

        @Parameter(
                names = {"-p", "--password"},
                description = "The user's password",
                required = true,
                validateWith = NonEmptyStringValidator.class
        )
        String password;

        @Parameter(
                names = {"-r", "--role"},
                description = "The user's role [writingteam, admin]"
        )
        List<String> roles;

        @Override
        public void run() {
            UserStore userStore = new UserStore(environment.getConnectionFactory());
            User user = User.builder()
                    .setUsername(username.trim())
                    .setPassword(password.trim())
                    .setRoles(roles)
                    .build();
            userStore.addUser(user);
        }
    }

    private void run(String[] args) {
        Map<String, Command> commands = ImmutableMap.of(
                "initdb", new CommandInitDb(),
                "resethunt", new CommandResetHunt(),
                "adduser", new CommandAddUser()
        );
        for (Command command : commands.values()) {
            jCommander.addCommand(command);
        }
        try {
            jCommander.parse(args);
            String parsedCommand = jCommander.getParsedCommand();
            if (parsedCommand == null) {
                throw new RuntimeException("No command was specified");
            }
            if (commands.get(parsedCommand) == null) {
                throw new RuntimeException("Unrecognized command " + parsedCommand);
            }
            commands.get(parsedCommand).run();
        } catch (Exception e) {
            jCommander.usage();
            throw e;
        }
    }

    public static void main(String[] args) {
        new CubeTool().run(args);
    }
}
