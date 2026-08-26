package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.persistence.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcUserRepository extends JdbcRepositorySupport implements UserRepository {
    private static final String SELECT_WITH_ROLE = """
            SELECT u.user_id, u.username, u.password_hash, u.first_name, u.last_name, u.active,
                   u.created_at, u.updated_at, r.role_id, r.name AS role_name,
                   r.description AS role_description
            FROM APP_USER u
            JOIN "ROLE" r ON r.role_id = u.role_id
            """;

    public JdbcUserRepository() {
    }

    public JdbcUserRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public User save(User user) throws SQLException {
        requireNew(user.getUserId(), "User");
        if (user.getRole() == null || user.getRole().getRoleId() == null) {
            throw new IllegalArgumentException("User requires a persisted role");
        }
        long id = executeInsert("""
                INSERT INTO APP_USER
                    (role_id, username, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, statement -> {
            statement.setLong(1, user.getRole().getRoleId());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getFirstName());
            statement.setString(5, user.getLastName());
            statement.setBoolean(6, user.isActive());
        });
        return findById(id).orElseThrow(() -> new SQLException("Saved user was not found"));
    }

    @Override
    public Optional<User> findByUsername(String username) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WITH_ROLE + " WHERE u.username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WITH_ROLE + " ORDER BY u.user_id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(map(resultSet));
            }
        }
        return users;
    }

    private Optional<User> findById(long userId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WITH_ROLE + " WHERE u.user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private static User map(ResultSet resultSet) throws SQLException {
        Role role = new Role(resultSet.getLong("role_id"), resultSet.getString("role_name"),
                resultSet.getString("role_description"));
        return new User(
                resultSet.getLong("user_id"), role, resultSet.getString("username"),
                resultSet.getString("password_hash"), resultSet.getString("first_name"),
                resultSet.getString("last_name"), resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
