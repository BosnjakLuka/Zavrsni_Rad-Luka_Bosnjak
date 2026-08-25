package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.persistence.repository.ShapeRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Optional;

public final class JdbcShapeRepository extends JdbcRepositorySupport implements ShapeRepository {
    public JdbcShapeRepository() {
    }

    public JdbcShapeRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Shape save(Shape shape) throws SQLException {
        requireNew(shape.getShapeId(), "Shape");
        long id;
        try (Connection connection = connectionProvider.getConnection()) {
            id = insertOnOpenConnection(connection, shape);
        }
        return findById(id).orElseThrow(() -> new SQLException("Saved shape was not found"));
    }

    long insertOnOpenConnection(Connection connection, Shape shape) throws SQLException {
        requireNew(shape.getShapeId(), "Shape");
        return executeInsert(connection, """
                INSERT INTO SHAPE (shape_type, shape_subtype, dimension_a, dimension_b, dimension_c,
                    created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """, statement -> {
            statement.setString(1, shape.getShapeType().name());
            statement.setString(2, shape.getShapeSubtype() == null ? null : shape.getShapeSubtype().name());
            statement.setDouble(3, shape.getDimensionA());
            setNullableDouble(statement, 4, shape.getDimensionB());
            setNullableDouble(statement, 5, shape.getDimensionC());
            statement.setTimestamp(6, shape.getDeletedAt() == null ? null : Timestamp.valueOf(shape.getDeletedAt()));
        });
    }

    @Override
    public Optional<Shape> findById(long shapeId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM SHAPE WHERE shape_id = ?")) {
            statement.setLong(1, shapeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private static Shape map(ResultSet resultSet) throws SQLException {
        String subtype = resultSet.getString("shape_subtype");
        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        return new Shape(
                resultSet.getLong("shape_id"), ShapeType.valueOf(resultSet.getString("shape_type")),
                subtype == null ? null : ShapeSubtype.valueOf(subtype), resultSet.getDouble("dimension_a"),
                nullableDouble(resultSet, "dimension_b"), nullableDouble(resultSet, "dimension_c"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime(),
                deletedAt == null ? null : deletedAt.toLocalDateTime()
        );
    }

    private static void setNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DOUBLE);
        } else {
            statement.setDouble(index, value);
        }
    }

    private static Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }
}
