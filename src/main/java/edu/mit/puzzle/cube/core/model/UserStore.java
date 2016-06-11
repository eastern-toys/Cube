package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;

import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.util.ByteSource;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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

    public void addUser(User user) {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement insertUserStatement = connection.prepareStatement(
                        "INSERT INTO users (username, password, password_salt, teamId) VALUES (?,?,?,?)");
                PreparedStatement insertUserRoleStatement = connection.prepareStatement(
                        "INSERT INTO user_roles (username, role_name) VALUES (?,?)")
        ) {
            ByteSource saltByteSource = hashService.getRandomNumberGenerator().nextBytes();
            String salt = Base64.getEncoder().encodeToString(saltByteSource.getBytes());
            Hash passwordHash = hashService.computeHash(new HashRequest.Builder()
                    .setSource(user.getPassword())
                    .setSalt(salt)
                    .build()
            );

            connection.setAutoCommit(false);

            insertUserStatement.setString(1, user.getUsername());
            insertUserStatement.setString(2, passwordHash.toHex());
            insertUserStatement.setString(3, salt);
            if (user.getTeamId() != null) {
                insertUserStatement.setString(4, user.getTeamId());
            } else {
                insertUserStatement.setNull(4, Types.NULL);
            }
            insertUserStatement.executeUpdate();

            if (user.getRoles() != null) {
                insertUserRoleStatement.setString(1, user.getUsername());
                for (String role : user.getRoles()) {
                    insertUserRoleStatement.setString(2, role);
                    insertUserRoleStatement.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    e,
                    "Failed to add user to the database");
        }
    }

    public void addUserPermissions(String username, List<String> permissions) {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement insertPermissionStatement = connection.prepareStatement(
                        "INSERT INTO users_permissions (username, permission) VALUES (?,?)");
        ) {
            connection.setAutoCommit(false);

            insertPermissionStatement.setString(1, username);
            for (String permission : permissions) {
                insertPermissionStatement.setString(2, permission);
                insertPermissionStatement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    e,
                    "Failed to add permissions to the database");
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

        Map<String, Object> row = resultTable.row(0);
        return User.builder()
                .setUsername((String) row.get("username"))
                .setTeamId((String) row.get("teamId"))
                .build();
    }
}
