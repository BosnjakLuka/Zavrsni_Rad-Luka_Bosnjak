package hr.lukabosnjak.config;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {
    private static final String TEST_JDBC_URL = "jdbc:h2:mem:database-config-smoke";

    @Test
    void opensAndClosesInMemoryConnection() throws Exception {
        Connection connection;

        try (Connection openedConnection = DatabaseConfig.getConnection(TEST_JDBC_URL)) {
            connection = openedConnection;
            assertFalse(connection.isClosed());
        }

        assertTrue(connection.isClosed());
    }
}
