package hr.lukabosnjak.config;

import org.h2.tools.RunScript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public final class DatabaseInitializer {
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";
    private static final Set<String> EXPECTED_TABLES = Set.of(
            "ROLE",
            "APP_USER",
            "MATERIAL_TYPE",
            "CNC_MACHINE",
            "TOOL",
            "MATERIAL_SHEET",
            "MACHINING_PARAMETERS",
            "SHAPE",
            "MACHINING_JOB"
    );

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            initialize(connection);
        }
    }

    static void initialize(String jdbcUrl) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection(jdbcUrl)) {
            initialize(connection);
        }
    }

    private static void initialize(Connection connection) throws SQLException {
        Set<String> existingTables = readPublicTableNames(connection);

        if (existingTables.containsAll(EXPECTED_TABLES)) {
            return;
        }

        if (!existingTables.isEmpty()) {
            throw incompleteSchemaException(existingTables);
        }

        executeSchema(connection);

        Set<String> initializedTables = readPublicTableNames(connection);
        if (!initializedTables.containsAll(EXPECTED_TABLES)) {
            throw incompleteSchemaException(initializedTables);
        }
    }

    private static void executeSchema(Connection connection) throws SQLException {
        InputStream schemaStream = DatabaseInitializer.class.getResourceAsStream(SCHEMA_RESOURCE);
        if (schemaStream == null) {
            throw new SQLException("Schema resource not found: " + SCHEMA_RESOURCE);
        }

        try (Reader reader = new InputStreamReader(schemaStream, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (java.io.IOException exception) {
            throw new SQLException("Failed to close schema resource: " + SCHEMA_RESOURCE, exception);
        }
    }

    private static Set<String> readPublicTableNames(Connection connection) throws SQLException {
        Set<String> tableNames = new HashSet<>();

        try (ResultSet tables = connection.getMetaData().getTables(null, "PUBLIC", "%", null)) {
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }
        }

        return tableNames;
    }

    private static SQLException incompleteSchemaException(Set<String> existingTables) {
        Set<String> missingTables = new TreeSet<>(EXPECTED_TABLES);
        missingTables.removeAll(existingTables);
        return new SQLException("Database schema is incomplete. Missing tables: " + missingTables);
    }
}
