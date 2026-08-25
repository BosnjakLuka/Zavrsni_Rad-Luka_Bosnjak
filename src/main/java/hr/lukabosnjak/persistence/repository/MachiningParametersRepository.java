package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.MachiningParameters;

import java.sql.SQLException;
import java.util.Optional;

public interface MachiningParametersRepository {
    MachiningParameters save(MachiningParameters machiningParameters) throws SQLException;

    Optional<MachiningParameters> findById(long machiningParametersId) throws SQLException;
}
