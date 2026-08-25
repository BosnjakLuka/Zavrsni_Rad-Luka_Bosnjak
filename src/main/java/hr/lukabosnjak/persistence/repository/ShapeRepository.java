package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.Shape;

import java.sql.SQLException;
import java.util.Optional;

public interface ShapeRepository {
    Shape save(Shape shape) throws SQLException;

    Optional<Shape> findById(long shapeId) throws SQLException;
}
