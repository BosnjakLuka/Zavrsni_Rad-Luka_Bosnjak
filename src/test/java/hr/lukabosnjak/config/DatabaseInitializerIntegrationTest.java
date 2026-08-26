package hr.lukabosnjak.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializerIntegrationTest {
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

    @TempDir
    Path tempDirectory;

    @Test
    void initializesNewFileDatabaseAndPreservesExistingData() throws Exception {
        String jdbcUrl = createFileJdbcUrl(tempDirectory.resolve("cnc-optimizer"));

        DatabaseInitializer.initialize(jdbcUrl);

        try (Connection connection = DatabaseConfig.getConnection(jdbcUrl)) {
            assertEquals(EXPECTED_TABLES, readPublicTableNames(connection));
            insertMaterialType(connection, "Test material");
            insertCompleteMachine(connection);
            simulateLegacyMachineConstraints(connection);
        }

        DatabaseInitializer.initialize(jdbcUrl);

        try (Connection connection = DatabaseConfig.getConnection(jdbcUrl)) {
            assertEquals(EXPECTED_TABLES, readPublicTableNames(connection));
            assertEquals(1, countMaterialTypes(connection, "Test material"));
            assertEquals(1, countMachines(connection));
            assertTrue(machineLimitColumnsAreNullable(connection));
        }
    }

    private String createFileJdbcUrl(Path databasePath) {
        String normalizedPath = databasePath.toAbsolutePath().toString().replace('\\', '/');
        return "jdbc:h2:file:" + normalizedPath;
    }

    private Set<String> readPublicTableNames(Connection connection) throws Exception {
        Set<String> tableNames = new HashSet<>();

        try (ResultSet tables = connection.getMetaData().getTables(null, "PUBLIC", "%", null)) {
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }
        }

        return tableNames;
    }

    private void insertMaterialType(Connection connection, String name) throws Exception {
        String sql = """
                INSERT INTO MATERIAL_TYPE (name, created_at, updated_at)
                VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private int countMaterialTypes(Connection connection, String name) throws Exception {
        String sql = "SELECT COUNT(*) FROM MATERIAL_TYPE WHERE name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private void insertCompleteMachine(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO CNC_MACHINE
                    (name, model, controller, work_area_x, work_area_y, work_area_z,
                     max_feed_rate, min_spindle_speed, max_spindle_speed, created_at, updated_at)
                VALUES ('Legacy machine', 'Legacy model', 'Legacy controller', 100, 200, 50,
                        1000, 500, 20000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            statement.executeUpdate();
        }
    }

    private void simulateLegacyMachineConstraints(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE CNC_MACHINE ALTER COLUMN work_area_z SET NOT NULL");
            statement.execute("ALTER TABLE CNC_MACHINE ALTER COLUMN max_feed_rate SET NOT NULL");
            statement.execute("ALTER TABLE CNC_MACHINE ALTER COLUMN min_spindle_speed SET NOT NULL");
            statement.execute("ALTER TABLE CNC_MACHINE ALTER COLUMN max_spindle_speed SET NOT NULL");
        }
    }

    private int countMachines(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM CNC_MACHINE");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean machineLimitColumnsAreNullable(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = 'CNC_MACHINE'
                  AND COLUMN_NAME IN ('WORK_AREA_Z', 'MAX_FEED_RATE',
                                      'MIN_SPINDLE_SPEED', 'MAX_SPINDLE_SPEED')
                  AND IS_NULLABLE = 'YES'
                """);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1) == 4;
        }
    }
}
