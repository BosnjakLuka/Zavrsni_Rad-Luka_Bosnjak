package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.persistence.repository.RoleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcRoleRepository extends JdbcRepositorySupport implements RoleRepository {
    public JdbcRoleRepository() {
    }

    public JdbcRoleRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
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

    static Role map(ResultSet resultSet) throws SQLException {
        return new Role(resultSet.getLong("role_id"), resultSet.getString("name"),
                resultSet.getString("description"));
    }
}
