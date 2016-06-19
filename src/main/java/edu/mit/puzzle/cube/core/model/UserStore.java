package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.permissions.CubePermission;
import edu.mit.puzzle.cube.core.permissions.CubeRole;
import edu.mit.puzzle.cube.core.permissions.RolesAndInstanceLevelPermissions;

import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.util.ByteSource;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserStore {

    private final ConnectionFactory connectionFactory;
    private final DefaultHashService hashService;

    public UserStore(
        ConnectionFactory connectionFactory
    ) {
        this.connectionFactory = connectionFactory;

        hashService = new DefaultHashService();
        hashService.setHashAlgorithmName("SHA-512");
    }

    private String generatePasswordSalt() {
        ByteSource saltByteSource = hashService.getRandomNumberGenerator().nextBytes();
        return Base64.getEncoder().encodeToString(saltByteSource.getBytes());
    }

    private Hash saltAndHashPassword(String password, String salt) {
        return hashService.computeHash(new HashRequest.Builder()
                .setSource(password)
                .setSalt(salt)
                .build());
    }

    public void addUser(User user) {
        String passwordSalt = generatePasswordSalt();
        Hash passwordHash = saltAndHashPassword(user.getPassword(), passwordSalt);
        RolesAndInstanceLevelPermissions rolesAndPermissions =
                RolesAndInstanceLevelPermissions.forUser(user);

        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement insertUserStatement = connection.prepareStatement(
                        "INSERT INTO users (username, password, password_salt, teamId) VALUES (?,?,?,?)");
                PreparedStatement insertUserRoleStatement = connection.prepareStatement(
                        "INSERT INTO user_roles (username, role_name) VALUES (?,?)");
                PreparedStatement insertPermissionStatement = connection.prepareStatement(
                        "INSERT INTO users_permissions (username, permission) VALUES (?,?)")
        ) {
            connection.setAutoCommit(false);

            insertUserStatement.setString(1, user.getUsername());
            insertUserStatement.setString(2, passwordHash.toHex());
            insertUserStatement.setString(3, passwordSalt);
            if (user.getTeamId() != null) {
                insertUserStatement.setString(4, user.getTeamId());
            } else {
                insertUserStatement.setNull(4, Types.NULL);
            }
            insertUserStatement.executeUpdate();

            insertUserRoleStatement.setString(1, user.getUsername());
            for (CubeRole role : rolesAndPermissions.getRoles()) {
                insertUserRoleStatement.setString(2, role.getName());
                insertUserRoleStatement.executeUpdate();
            }

            insertPermissionStatement.setString(1, user.getUsername());
            for (CubePermission permission : rolesAndPermissions.getInstanceLevelPermissions()) {
                insertPermissionStatement.setString(2, permission.getWildcardString());
                insertPermissionStatement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    e,
                    "Failed to add user to the database");
        }
    }

    public User getUser(String username) {
        Table<Integer,String,Object> resultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM users WHERE username = ?",
                ImmutableList.of(username)
        );

        if (resultTable.rowKeySet().size() == 0) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_NOT_FOUND,
                    String.format("The username '%s' does not exist", username));
        } else if (resultTable.rowKeySet().size() > 1) {
            throw new RuntimeException("Primary key violation in application layer");
        }

        Table<Integer,String,Object> rolesResultTable = DatabaseHelper.query(
                connectionFactory,
                "SELECT role_name FROM user_roles WHERE username = ?",
                ImmutableList.of(username)
        );
        List<String> roles = rolesResultTable.column("role_name").values().stream()
                .map(o -> (String) o)
                .collect(Collectors.toList());

        Map<String, Object> row = resultTable.row(0);
        return User.builder()
                .setUsername((String) row.get("username"))
                .setTeamId((String) row.get("teamId"))
                .setRoles(roles)
                .build();
    }

    public List<User> getAllUsers() {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement selectUsersStatement = connection.prepareStatement(
                        "SELECT * FROM users");
                PreparedStatement selectUserRolesStatement = connection.prepareStatement(
                        "SELECT * FROM user_roles")
        ) {
            ResultSet resultSet = selectUserRolesStatement.executeQuery();
            // Map from username to that user's roles.
            Multimap<String, String> userRolesMap = HashMultimap.create();
            while (resultSet.next()) {
                userRolesMap.put(resultSet.getString("username"), resultSet.getString("role_name"));
            }

            resultSet = selectUsersStatement.executeQuery();
            ImmutableList.Builder<User> users = ImmutableList.builder();
            while (resultSet.next()) {
                User.Builder user = User.builder()
                        .setUsername(resultSet.getString("username"));

                String teamId = resultSet.getString("teamId");
                if (teamId != null && !teamId.isEmpty()) {
                    user.setTeamId(teamId);
                }

                user.setRoles(ImmutableList.copyOf(userRolesMap.get(resultSet.getString("username"))));

                users.add(user.build());
            }
            return users.build();
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.SERVER_ERROR_INTERNAL.getCode(),
                    e,
                    "Failed to read users");
        }
    }

    public boolean updateUser(User user) {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement selectUserStatement = connection.prepareStatement(
                        "SELECT * FROM users WHERE username = ?");
                PreparedStatement selectUserRolesStatement = connection.prepareStatement(
                        "SELECT role_name FROM user_roles WHERE username = ?");
                PreparedStatement updatePasswordStatement = connection.prepareStatement(
                        "UPDATE users SET password = ?, password_salt = ? " +
                        "WHERE username = ?");
                PreparedStatement deleteUserRolesStatement = connection.prepareStatement(
                        "DELETE FROM user_roles WHERE username = ?");
                PreparedStatement insertUserRoleStatement = connection.prepareStatement(
                        "INSERT INTO user_roles (username, role_name) VALUES (?,?)");
                PreparedStatement deleteUserPermissionsStatement = connection.prepareStatement(
                        "DELETE FROM users_permissions WHERE username = ?");
                PreparedStatement insertPermissionStatement = connection.prepareStatement(
                        "INSERT INTO users_permissions (username, permission) VALUES (?,?)")
        ) {
            connection.setAutoCommit(false);

            selectUserStatement.setString(1, user.getUsername());
            ResultSet resultSet = selectUserStatement.executeQuery();
            int rowCount = 0;
            String existingPasswordHash = null;
            String existingPasswordSalt = null;
            String existingTeamId = null;
            while (resultSet.next()) {
                rowCount++;
                if (rowCount > 1) {
                    throw new RuntimeException("Primary key violation in application layer");
                }
                existingPasswordHash = resultSet.getString("password");
                existingPasswordSalt = resultSet.getString("password_salt");
                existingTeamId = resultSet.getString("teamId");
            }
            if (rowCount == 0) {
                throw new ResourceException(
                        Status.CLIENT_ERROR_NOT_FOUND,
                        String.format("The username '%s' does not exist", user.getUsername()));
            }

            if (user.getTeamId() != null && !user.getTeamId().equals(existingTeamId)) {
                throw new ResourceException(
                        Status.CLIENT_ERROR_BAD_REQUEST,
                        "Changing the team id associated with a user is not currently supported");
            }

            selectUserRolesStatement.setString(1, user.getUsername());
            resultSet = selectUserRolesStatement.executeQuery();
            Set<String> existingRoles = new HashSet<>();
            while (resultSet.next()) {
                existingRoles.add(resultSet.getString("role_name"));
            }

            boolean passwordUpdated = false;
            if (user.getPassword() != null) {
                String passwordHash = saltAndHashPassword(user.getPassword(), existingPasswordSalt).toHex();
                if (passwordHash.equals(existingPasswordHash)) {
                    throw new ResourceException(
                            Status.CLIENT_ERROR_BAD_REQUEST,
                            "A password change was requested, but the new password was the same as the old one");
                }

                String newPasswordSalt = generatePasswordSalt();
                passwordHash = saltAndHashPassword(user.getPassword(), newPasswordSalt).toHex();
                updatePasswordStatement.setString(1, passwordHash);
                updatePasswordStatement.setString(2, newPasswordSalt);
                updatePasswordStatement.setString(3, user.getUsername());
                passwordUpdated = updatePasswordStatement.executeUpdate() > 0;
            }

            boolean rolesUpdated = false;
            if (user.getRoles() != null
                    && !ImmutableSet.copyOf(user.getRoles()).equals(existingRoles)) {
                rolesUpdated = true;

                // teamId may not have been specified in the update, make sure we include it when
                // computing permissions.
                User userForComputingPermissions = user;
                if (existingTeamId != null && userForComputingPermissions.getTeamId() == null) {
                    userForComputingPermissions = userForComputingPermissions.toBuilder()
                            .setTeamId(existingTeamId)
                            .build();
                }
                RolesAndInstanceLevelPermissions rolesAndPermissions =
                        RolesAndInstanceLevelPermissions.forUser(userForComputingPermissions);

                deleteUserRolesStatement.setString(1, user.getUsername());
                deleteUserRolesStatement.executeUpdate();
                deleteUserPermissionsStatement.setString(1, user.getUsername());
                deleteUserPermissionsStatement.executeUpdate();

                insertUserRoleStatement.setString(1, user.getUsername());
                for (CubeRole role : rolesAndPermissions.getRoles()) {
                    insertUserRoleStatement.setString(2, role.getName());
                    insertUserRoleStatement.executeUpdate();
                }

                insertPermissionStatement.setString(1, user.getUsername());
                for (CubePermission permission : rolesAndPermissions.getInstanceLevelPermissions()) {
                    insertPermissionStatement.setString(2, permission.getWildcardString());
                    insertPermissionStatement.executeUpdate();
                }
            }

            connection.commit();

            return passwordUpdated || rolesUpdated;
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    e,
                    "Failed to update user");
        }

    }
}
