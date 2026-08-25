package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

abstract class JdbcRepositorySupport {
    protected final ConnectionProvider connectionProvider;

    protected JdbcRepositorySupport() {
        this(DatabaseConfig::getConnection);
    }

    protected JdbcRepositorySupport(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    protected void requireNew(Object id, String entityName) {
        if (id != null) {
            throw new IllegalArgumentException(entityName + " is already persisted");
        }
    }

    protected long executeInsert(String sql, StatementBinder binder) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return executeInsert(connection, sql, binder);
        }
    }

    protected long executeInsert(Connection connection, String sql, StatementBinder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Insert did not return a generated key");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    @FunctionalInterface
    protected interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
