package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.persistence.repository.RoleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcRoleRepository extends JdbcRepositorySupport implements RoleRepository {
    public JdbcRoleRepository() {
    }

    public JdbcRoleRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Role save(Role role) throws SQLException {
        requireNew(role.getRoleId(), "Role");
        long id = executeInsert("INSERT INTO \"ROLE\" (name, description) VALUES (?, ?)", statement -> {
            statement.setString(1, role.getName());
            statement.setString(2, role.getDescription());
        });
        return findById(id).orElseThrow(() -> new SQLException("Saved role was not found"));
    }

    @Override
    public Optional<Role> findByName(String name) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT role_id, name, description FROM \"ROLE\" WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Role> findAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT role_id, name, description FROM \"ROLE\" ORDER BY role_id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                roles.add(map(resultSet));
            }
        }
        return roles;
    }

    @Override
    public boolean deleteIfUnused(String name) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM "ROLE"
                     WHERE name = ?
                       AND NOT EXISTS (
                           SELECT 1 FROM APP_USER user_record
                           WHERE user_record.role_id = "ROLE".role_id
                       )
                     """)) {
            statement.setString(1, name);
            return statement.executeUpdate() == 1;
        }
    }

    private Optional<Role> findById(long roleId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT role_id, name, description FROM \"ROLE\" WHERE role_id = ?")) {
            statement.setLong(1, roleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    static Role map(ResultSet resultSet) throws SQLException {
        return new Role(resultSet.getLong("role_id"), resultSet.getString("name"),
                resultSet.getString("description"));
    }
}
