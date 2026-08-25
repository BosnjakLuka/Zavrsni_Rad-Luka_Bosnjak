package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.persistence.repository.MaterialSheetRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public final class JdbcMaterialSheetRepository extends JdbcRepositorySupport implements MaterialSheetRepository {
    private static final String SELECT_WITH_MATERIAL = """
            SELECT ms.material_sheet_id, ms.width, ms.height, ms.thickness,
                   ms.created_at AS sheet_created_at, ms.updated_at AS sheet_updated_at,
                   mt.material_type_id, mt.name AS material_name, mt.description AS material_description,
                   mt.created_at AS material_created_at, mt.updated_at AS material_updated_at,
                   mt.deleted_at AS material_deleted_at
            FROM MATERIAL_SHEET ms
            JOIN MATERIAL_TYPE mt ON mt.material_type_id = ms.material_type_id
            """;

    public JdbcMaterialSheetRepository() {
    }

    public JdbcMaterialSheetRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public MaterialSheet save(MaterialSheet sheet) throws SQLException {
        requireNew(sheet.getMaterialSheetId(), "Material sheet");
        requirePersistedMaterialType(sheet.getMaterialType());
        long id;
        try (Connection connection = connectionProvider.getConnection()) {
            id = insertOnOpenConnection(connection, sheet);
        }
        return findById(id).orElseThrow(() -> new SQLException("Saved material sheet was not found"));
    }

    long insertOnOpenConnection(Connection connection, MaterialSheet sheet) throws SQLException {
        return executeInsert(connection, """
                INSERT INTO MATERIAL_SHEET
                    (material_type_id, width, height, thickness, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, statement -> {
            statement.setLong(1, sheet.getMaterialType().getMaterialTypeId());
            statement.setDouble(2, sheet.getWidth());
            statement.setDouble(3, sheet.getHeight());
            statement.setDouble(4, sheet.getThickness());
        });
    }

    @Override
    public Optional<MaterialSheet> findById(long materialSheetId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_WITH_MATERIAL + " WHERE ms.material_sheet_id = ?")) {
            statement.setLong(1, materialSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private static MaterialSheet map(ResultSet resultSet) throws SQLException {
        Timestamp deletedAt = resultSet.getTimestamp("material_deleted_at");
        MaterialType materialType = new MaterialType(
                resultSet.getLong("material_type_id"), resultSet.getString("material_name"),
                resultSet.getString("material_description"),
                resultSet.getTimestamp("material_created_at").toLocalDateTime(),
                resultSet.getTimestamp("material_updated_at").toLocalDateTime(),
                deletedAt == null ? null : deletedAt.toLocalDateTime()
        );
        return new MaterialSheet(
                resultSet.getLong("material_sheet_id"), materialType, resultSet.getDouble("width"),
                resultSet.getDouble("height"), resultSet.getDouble("thickness"),
                resultSet.getTimestamp("sheet_created_at").toLocalDateTime(),
                resultSet.getTimestamp("sheet_updated_at").toLocalDateTime()
        );
    }

    private static void requirePersistedMaterialType(MaterialType materialType) {
        if (materialType == null || materialType.getMaterialTypeId() == null) {
            throw new IllegalArgumentException("Material sheet requires a persisted material type");
        }
    }
}
