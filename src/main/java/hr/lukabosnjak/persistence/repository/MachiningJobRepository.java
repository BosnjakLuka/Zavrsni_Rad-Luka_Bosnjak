package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.MachiningJob;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MachiningJobRepository {
    MachiningJob save(MachiningJob machiningJob) throws SQLException;

    Optional<MachiningJob> findById(long machiningJobId) throws SQLException;

    List<MachiningJob> findAll() throws SQLException;
}
