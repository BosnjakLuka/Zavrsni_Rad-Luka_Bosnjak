package hr.lukabosnjak.config;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseSchemaIntegrationTest {
    private static final String TEST_JDBC_URL = "jdbc:h2:mem:schema-integration-test";
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

    @Test
    void createsCompleteV1SchemaAndSeedsRoles() throws Exception {
        try (Connection connection = DatabaseConfig.getConnection(TEST_JDBC_URL)) {
            executeSchema(connection);

            Set<String> actualTables = readPublicTableNames(connection);
            assertEquals(EXPECTED_TABLES, actualTables);
            assertFalse(actualTables.contains("USER"));
            assertEquals(Set.of("USER", "ADMIN"), readRoleNames(connection));
            assertEquals(0, countUsers(connection));
        }
    }

    private void executeSchema(Connection connection) throws Exception {
        InputStream schemaStream = getClass().getResourceAsStream("/db/schema.sql");
        assertNotNull(schemaStream, "db/schema.sql must be available on the test classpath");

        try (Reader reader = new InputStreamReader(schemaStream, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
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

    private Set<String> readRoleNames(Connection connection) throws Exception {
        Set<String> roleNames = new HashSet<>();

        try (PreparedStatement statement = connection.prepareStatement("SELECT name FROM \"ROLE\"");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                roleNames.add(resultSet.getString("name"));
            }
        }

        return roleNames;
    }

    private int countUsers(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM APP_USER");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
