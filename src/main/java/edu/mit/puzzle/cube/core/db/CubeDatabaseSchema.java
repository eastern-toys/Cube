package edu.mit.puzzle.cube.core.db;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Resources;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CubeDatabaseSchema {
    private static final String VAR_AUTO_INCREMENT_TYPE = "auto_increment_type";

    private final String schema;

    public CubeDatabaseSchema(String jdbcDriverClassName) {
        Map<String, String> schemaTemplateMap = new HashMap<>();
        switch (jdbcDriverClassName) {
        case "org.sqlite.JDBC":
            schemaTemplateMap.put(VAR_AUTO_INCREMENT_TYPE, "INTEGER");
            break;
        case "org.postgresql.Driver":
            schemaTemplateMap.put(VAR_AUTO_INCREMENT_TYPE, "SERIAL");
            break;
        case "com.mysql.jdbc.Driver":
            schemaTemplateMap.put(VAR_AUTO_INCREMENT_TYPE, "INT NOT NULL AUTO_INCREMENT");
            break;
        default:
            throw new RuntimeException(
                    "Unsupported database driver: " + jdbcDriverClassName);
        }

        URL schemaUrl = Resources.getResource("cube.sql");
        String schemaTemplate;
        try {
            schemaTemplate = Resources.toString(schemaUrl, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        schema = new StrSubstitutor(schemaTemplateMap).replace(schemaTemplate);
    }

    public void execute(Connection connection) throws SQLException {
        Splitter schemaSplitter = Splitter.on(";").omitEmptyStrings().trimResults();
        for (String schemaStatement : schemaSplitter.split(schema)) {
            try (PreparedStatement statement = connection.prepareStatement(schemaStatement)) {
                statement.execute();
            }
        }
    }
}
