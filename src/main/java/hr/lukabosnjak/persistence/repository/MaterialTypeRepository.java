package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.MaterialType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MaterialTypeRepository {
    MaterialType save(MaterialType materialType) throws SQLException;
    MaterialType update(MaterialType materialType) throws SQLException;

    Optional<MaterialType> findById(long materialTypeId) throws SQLException;

    List<MaterialType> findAll() throws SQLException;
}
