package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.InMemoryConnectionFactory;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.restlet.resource.ResourceException;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserStoreTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private ConnectionFactory connectionFactory;
    private UserStore userStore;

    private static String TEST_TEAM_ID = "testerteam";
    private static String TEST_PUZZLE_ID = "a_test_puzzle";

    @Before
    public void setup() throws SQLException {
        connectionFactory = new InMemoryConnectionFactory(
                new StandardVisibilityStatusSet(),
                Lists.newArrayList(TEST_TEAM_ID),
                Lists.newArrayList(TEST_PUZZLE_ID));
        userStore = new UserStore(connectionFactory);
    }

    @Test
    public void addAndGetUser() {
        User user = User.builder()
                .setUsername("testuser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("writingteam"))
                .build();
        userStore.addUser(user);

        User readUser = userStore.getUser("testuser");
        assertEquals(user.getUsername(), readUser.getUsername());
        assertNull(readUser.getPassword());
        assertNull(readUser.getRoles());
    }

    @Test
    public void getUser_doesNotExist() {
        exception.expect(ResourceException.class);
        userStore.getUser("testuser");
    }

    @Test
    public void addUser_roleDoesNotExist() {
        exception.expect(ResourceException.class);
        userStore.addUser(User.builder()
                .setUsername("testuser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("nonexistentrole"))
                .build());
    }
}
