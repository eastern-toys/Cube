package edu.mit.puzzle.cube.core.db;

import com.google.common.collect.ImmutableSet;

import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.realm.jdbc.JdbcRealm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

public class CubeJdbcRealm extends JdbcRealm {

    public CubeJdbcRealm() {
        super();
        setCredentialsMatcher(new HashedCredentialsMatcher("SHA-512"));
        setAuthorizationCachingEnabled(false);
        setPermissionsLookupEnabled(true);
        setSaltStyle(JdbcRealm.SaltStyle.COLUMN);
    }

    protected Set<String> getPermissions(
            Connection connection,
            String username,
            Collection<String> roleNames
    ) throws SQLException {
        ImmutableSet.Builder<String> permissions = ImmutableSet.builder();
        permissions.addAll(super.getPermissions(connection, username, roleNames));

        try (PreparedStatement query = connection.prepareStatement(
                "SELECT permission FROM users_permissions WHERE username = ?")) {
            query.setString(1, username);
            ResultSet resultSet = query.executeQuery();
            while (resultSet.next()) {
                permissions.add(resultSet.getString(1));
            }
        }

        return permissions.build();
    }
}
