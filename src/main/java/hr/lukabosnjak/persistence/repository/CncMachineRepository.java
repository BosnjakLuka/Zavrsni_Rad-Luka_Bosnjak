package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.CncMachine;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CncMachineRepository {
    CncMachine save(CncMachine cncMachine) throws SQLException;

    Optional<CncMachine> findById(long cncMachineId) throws SQLException;

    List<CncMachine> findAll() throws SQLException;
}
