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
    private static final String CNC_MACHINE_NULLABLE_LIMITS_MIGRATION =
            "/db/migration/12-02-cnc-machine-nullable-limits.sql";
    private static final String CATALOG_ACTIVE_STATUS_MIGRATION =
            "/db/migration/14-06-catalog-active-status.sql";
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

    public static void initialize(Connection connection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Connection is required");
        }
        Set<String> existingTables = readPublicTableNames(connection);

        if (!existingTables.isEmpty() && !existingTables.containsAll(EXPECTED_TABLES)) {
            throw incompleteSchemaException(existingTables);
        }

        if (existingTables.isEmpty()) {
            executeScript(connection, SCHEMA_RESOURCE);
        }

        Set<String> initializedTables = readPublicTableNames(connection);
        if (!initializedTables.containsAll(EXPECTED_TABLES)) {
            throw incompleteSchemaException(initializedTables);
        }

        executeScript(connection, CNC_MACHINE_NULLABLE_LIMITS_MIGRATION);
        executeScript(connection, CATALOG_ACTIVE_STATUS_MIGRATION);
    }

    private static void executeScript(Connection connection, String resourcePath) throws SQLException {
        InputStream schemaStream = DatabaseInitializer.class.getResourceAsStream(resourcePath);
        if (schemaStream == null) {
            throw new SQLException("Database script resource not found: " + resourcePath);
        }

        try (Reader reader = new InputStreamReader(schemaStream, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (java.io.IOException exception) {
            throw new SQLException("Failed to close database script resource: " + resourcePath, exception);
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
