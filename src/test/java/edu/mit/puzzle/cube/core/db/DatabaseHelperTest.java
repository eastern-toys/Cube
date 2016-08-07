package edu.mit.puzzle.cube.core.db;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class DatabaseHelperTest {

    private ConnectionFactory connectionFactory;

    private static String TEST_TEAM_ID = "testerteam";
    private static String TEST_PUZZLE_ID = "a_test_puzzle";

    @Before
    public void setup() throws SQLException {
        connectionFactory = new InMemoryConnectionFactory(
                new StandardVisibilityStatusSet(),
                Lists.newArrayList(TEST_TEAM_ID),
                Lists.newArrayList(Puzzle.create(TEST_PUZZLE_ID, "ANSWER")),
                ImmutableList.<User>of());
    }

    @Test
    public void testInsert() {
        Optional<Integer> id = DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO submissions (teamId, puzzleId, submission) VALUES (?,?,?)",
                Lists.newArrayList(TEST_TEAM_ID, TEST_PUZZLE_ID, "guess")
        );
        assertEquals(Optional.of(1), id);

        id = DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO submissions (teamId, puzzleId, submission) VALUES (?,?,?)",
                Lists.newArrayList(TEST_TEAM_ID, TEST_PUZZLE_ID, "guess")
        );
        assertEquals(Optional.of(2), id);

        id = DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO submissions (teamId, puzzleId, submission) VALUES (?,?,?)",
                Lists.newArrayList(TEST_TEAM_ID, TEST_PUZZLE_ID, "guess")
        );
        assertEquals(Optional.of(3), id);

        DatabaseHelper.insertBatch(
                connectionFactory,
                "INSERT INTO submissions (teamId, puzzleId, submission) VALUES (?,?,?)",
                Lists.newArrayList(
                        Lists.newArrayList(TEST_TEAM_ID, TEST_PUZZLE_ID, "guess"),
                        Lists.newArrayList(TEST_TEAM_ID, TEST_PUZZLE_ID, "guess"))
        );

        Table<Integer, String, Object> results = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM submissions",
                Lists.newArrayList()
        );
        assertEquals(5, results.rowKeySet().size());
    }

    @Test
    public void testUpdate() {
        DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO submissions (teamId, puzzleId, submission) VALUES (?,?,?)",
                Lists.newArrayList(TEST_TEAM_ID, TEST_PUZZLE_ID, "guess")
        );
        DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO submissions (teamId, puzzleId, submission) VALUES (?,?,?)",
                Lists.newArrayList(TEST_TEAM_ID, TEST_PUZZLE_ID, "guess")
        );
        Table<Integer, String, Object> results = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM submissions",
                Lists.newArrayList()
        );
        for (Object statusObject : results.column("status").values()) {
            assertEquals(SubmissionStatus.getDefault().toString(), (String) statusObject);
        }

        int updates = DatabaseHelper.update(
                connectionFactory,
                "UPDATE submissions SET status = ? WHERE teamId = ? AND puzzleId = ?",
                Lists.newArrayList(SubmissionStatus.ASSIGNED, TEST_TEAM_ID, TEST_PUZZLE_ID)
        );
        assertEquals(2, updates);

        results = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM submissions",
                Lists.newArrayList()
        );
        for (Object statusObject : results.column("status").values()) {
            assertEquals(SubmissionStatus.ASSIGNED.toString(), (String) statusObject);
        }
    }

}
