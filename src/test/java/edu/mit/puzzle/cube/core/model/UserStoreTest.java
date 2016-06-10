package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.InMemorySingleUnsharedConnectionFactory;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.restlet.resource.ResourceException;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class UserStoreTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private ConnectionFactory connectionFactory;
    private UserStore userStore;

    private static String TEST_TEAM_ID = "testerteam";
    private static String TEST_PUZZLE_ID = "a_test_puzzle";

    @Before
    public void setup() throws SQLException {
        connectionFactory = new InMemorySingleUnsharedConnectionFactory(
                new StandardVisibilityStatusSet(),
                Lists.newArrayList(TEST_TEAM_ID),
                Lists.newArrayList(TEST_PUZZLE_ID));
        userStore = new UserStore(connectionFactory);
    }

    @Test
    public void addAndGetUser() {
        User user = User.builder().setUsername("testuser").build();
        userStore.addUser(
                user,
                "testpassword",
                ImmutableList.of("writingteam"));
        assertEquals(user, userStore.getUser("testuser"));
    }

    @Test
    public void getUser_doesNotExist() {
        exception.expect(ResourceException.class);
        userStore.getUser("testuser");
    }

    @Test
    public void addUser_roleDoesNotExist() {
        exception.expect(ResourceException.class);
        User user = User.builder().setUsername("testuser").build();
        userStore.addUser(
                user,
                "testpassword",
                ImmutableList.of("nonexistentrole"));
    }
}
