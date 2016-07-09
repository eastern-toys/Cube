package edu.mit.puzzle.cube.core.db;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DatabaseHelper is a class of static methods that wrap common calls to query or update
 * an SQL database. This is done because constantly getting Connections and Statements and
 * properly setting up try-catch blocks is annoying. Dealing with ResultSet objects is also
 * annoying.
 *
 * This class makes some assumptions about how you want queried data back through its heavy
 * uses of the Google Guava interface Table<R,C,V>. This also assumes that retrieved data is
 * small enough to fit within the JVM memory, but this should be true for Mystery Hunts. (If
 * it's not, please reconsider the size/complexity of what you're doing.)
 */
public class DatabaseHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

    /**
     * Queries a database (connected to by a Connection from ConnectionFactory) with the given
     * query and parameters. The resulting table rows are keyed by integers starting from 0 and
     * the columns are the database columns SELECTed.
     *
     * @param connectionFactory Provides a Connection to the database
     * @param preparedQuery A String with a SELECT query
     * @param parameters The parameters to go into the query. If there are no parameters, pass in an empty List.
     * @return A Table with results. Row keys are Integers, column keys are the SELECTed columns.
     */
    public static Table<Integer,String,Object> query(
            ConnectionFactory connectionFactory,
            String preparedQuery,
            List<Object> parameters
    ) {
        AtomicInteger counter = new AtomicInteger(0);
        Function<ResultSet,Integer> keyFunction = rs -> counter.getAndIncrement();

        return query(connectionFactory, preparedQuery, parameters, keyFunction);
    }

    /**
     * Queries a database (connected to by a Connection from ConnectionFactory) with the given
     * query and parameters. The resulting table rows are keyed by the value of keyField for each
     * row. Throws an exception if a row has a null or duplicated value of keyField.
     *
     * @param connectionFactory Provides a Connection to the database
     * @param preparedQuery A String with a SELECT query
     * @param parameters The parameters to go into the query. If there are no parameters, pass in an empty List.
     * @param keyField The SELECTed column used to key the rows
     * @param <KEY_TYPE> The Class of the keyField, used to cast Objects into that Class.
     * @return A Table with results. Row keys are Integers, column keys are the SELECTed columns.
     */
    public static <KEY_TYPE> Table<KEY_TYPE,String,Object> query(
            ConnectionFactory connectionFactory,
            String preparedQuery,
            List<Object> parameters,
            String keyField
    ) {
        Function<ResultSet,KEY_TYPE> keyFunction = rs -> {
            try {
                return (KEY_TYPE) rs.getObject(keyField);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };

        return query(connectionFactory, preparedQuery, parameters, keyFunction);
    }

    public static Table<String,String,Object> query(
            ConnectionFactory connectionFactory,
            String preparedQuery,
            List<Object> parameters,
            List<String> keyFields
    ) {
        Function<ResultSet,String> keyFunction = rs -> {
            try {
                List<String> keyValues = Lists.newArrayList();
                for (String keyField : keyFields) {
                    keyValues.add(rs.getString(keyField));
                }
                return Joiner.on("-").join(keyValues);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };

        return query(connectionFactory, preparedQuery, parameters, keyFunction);
    }

    public static <KEY_TYPE> Table<KEY_TYPE,String,Object> query(
        ConnectionFactory connectionFactory,
        String preparedQuery,
        List<Object> parameters,
        Function<ResultSet,KEY_TYPE> keyFunction
    ) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(preparedQuery)) {

            for (int i = 0; i < parameters.size(); ++i) {
                statement.setObject(i + 1, parameters.get(i));
            }
            ResultSet rs = statement.executeQuery();

            List<String> columnKeys = Lists.newArrayList();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); ++i) {
                columnKeys.add(rs.getMetaData().getColumnName(i));
            }

            ImmutableTable.Builder<KEY_TYPE,String,Object> tableBuilder = ImmutableTable.builder();
            while (rs.next()) {
                KEY_TYPE rowKey = keyFunction.apply(rs);
                for (String columnKey : columnKeys) {
                    Object value = rs.getObject(columnKey);
                    if (value != null) {
                        tableBuilder.put(rowKey, columnKey, value);
                    }
                }
            }

            return tableBuilder.build();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static <MODEL_TYPE> List<MODEL_TYPE> query(
            ConnectionFactory connectionFactory,
            String preparedQuery,
            List<Object> parameters,
            Class<MODEL_TYPE> resultClass
    ) {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(preparedQuery)
        ) {
            for (int i = 0; i < parameters.size(); ++i) {
                statement.setObject(i + 1, parameters.get(i));
            }
            ResultSet rs = statement.executeQuery();

            ImmutableList.Builder<MODEL_TYPE> results = ImmutableList.builder();
            while (rs.next()) {
                ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    Object value;
                    switch (rs.getMetaData().getColumnType(i)) {
                    case Types.TIMESTAMP:
                        Instant instant = rs.getTimestamp(i).toInstant();
                        value = instant.getLong(ChronoField.INSTANT_SECONDS) * 1000
                                + instant.getLong(ChronoField.MILLI_OF_SECOND);
                        break;
                    default:
                        value = rs.getObject(i);
                        break;
                    }
                    objectNode.putPOJO(rs.getMetaData().getColumnName(i), value);
                }
                results.add(OBJECT_MAPPER.convertValue(objectNode, resultClass));
            }

            return results.build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Integer> updateBatch(
            ConnectionFactory connectionFactory,
            String preparedUpdate,
            List<List<Object>> parameterLists
    ) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(preparedUpdate)) {

            for (List<Object> parameters : parameterLists) {
                for (int i = 0; i < parameters.size(); ++i) {
                    statement.setObject(i + 1, parameters.get(i));
                }
                statement.addBatch();
            }

            connection.setAutoCommit(false);
            int[] updatedRowsArray = statement.executeBatch();
            connection.setAutoCommit(true);

            return IntStream.of(updatedRowsArray).boxed().collect(Collectors.toList());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Integer update(
            ConnectionFactory connectionFactory,
            String preparedUpdate,
            List<Object> parameters
    ) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(preparedUpdate)) {

            for (int i = 0; i < parameters.size(); ++i) {
                statement.setObject(i + 1, parameters.get(i));
            }

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static Optional<Integer> insert(
            ConnectionFactory connectionFactory,
            String preparedInsert,
            List<Object> parameters
    ) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(preparedInsert, Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < parameters.size(); ++i) {
                statement.setObject(i + 1, parameters.get(i));
            }

            int updates = statement.executeUpdate();
            if (updates < 1) {
                return Optional.empty();
            }

            ResultSet rs = statement.getGeneratedKeys();
            Optional<Integer> insertedId = Optional.empty();
            while (rs.next()) {
                if (rs.getMetaData().getColumnType(1) == Types.INTEGER) {
                    insertedId = Optional.of(rs.getInt(1));
                }
            }
            return insertedId;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void insertBatch(
            ConnectionFactory connectionFactory,
            String preparedInsert,
            List<List<Object>> parameterLists
    ) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(preparedInsert)) {

            for (List<Object> parameters : parameterLists) {
                for (int i = 0; i < parameters.size(); ++i) {
                    statement.setObject(i + 1, parameters.get(i));
                }
                statement.addBatch();
            }

            connection.setAutoCommit(false);
            statement.executeBatch();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
