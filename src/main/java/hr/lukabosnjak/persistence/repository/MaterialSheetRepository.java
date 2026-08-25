package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.MaterialSheet;

import java.sql.SQLException;
import java.util.Optional;

public interface MaterialSheetRepository {
    MaterialSheet save(MaterialSheet materialSheet) throws SQLException;

    Optional<MaterialSheet> findById(long materialSheetId) throws SQLException;
}
