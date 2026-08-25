package hr.lukabosnjak.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        }

        DatabaseInitializer.initialize(jdbcUrl);

        try (Connection connection = DatabaseConfig.getConnection(jdbcUrl)) {
            assertEquals(EXPECTED_TABLES, readPublicTableNames(connection));
            assertEquals(1, countMaterialTypes(connection, "Test material"));
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
}
