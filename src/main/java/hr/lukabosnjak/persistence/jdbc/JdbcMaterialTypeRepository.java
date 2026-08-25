package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.persistence.repository.MaterialTypeRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcMaterialTypeRepository extends JdbcRepositorySupport implements MaterialTypeRepository {
    public JdbcMaterialTypeRepository() {
    }

    public JdbcMaterialTypeRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public MaterialType save(MaterialType materialType) throws SQLException {
        requireNew(materialType.getMaterialTypeId(), "Material type");
        long id = executeInsert("""
                INSERT INTO MATERIAL_TYPE (name, description, created_at, updated_at, deleted_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """, statement -> {
            statement.setString(1, materialType.getName());
            statement.setString(2, materialType.getDescription());
            statement.setTimestamp(3, timestamp(materialType.getDeletedAt()));
        });
        return findById(id).orElseThrow(() -> new SQLException("Saved material type was not found"));
    }

    @Override
    public Optional<MaterialType> findById(long materialTypeId) throws SQLException {
        String sql = "SELECT * FROM MATERIAL_TYPE WHERE material_type_id = ?";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialTypeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<MaterialType> findAll() throws SQLException {
        List<MaterialType> materialTypes = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM MATERIAL_TYPE ORDER BY material_type_id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                materialTypes.add(map(resultSet));
            }
        }
        return materialTypes;
    }

    static MaterialType map(ResultSet resultSet) throws SQLException {
        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        return new MaterialType(
                resultSet.getLong("material_type_id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime(),
                deletedAt == null ? null : deletedAt.toLocalDateTime()
        );
    }

    private static Timestamp timestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
