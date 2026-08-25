package hr.lukabosnjak.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {
    private static final String JDBC_URL = "jdbc:h2:file:./data/cnc-optimizer";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        return getConnection(JDBC_URL);
    }

    static Connection getConnection(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, USER, PASSWORD);
    }
}
