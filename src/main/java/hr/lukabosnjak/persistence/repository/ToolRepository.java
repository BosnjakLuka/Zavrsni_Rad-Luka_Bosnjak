package hr.lukabosnjak.persistence.repository;

import hr.lukabosnjak.domain.entities.Tool;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ToolRepository {
    Tool save(Tool tool) throws SQLException;
    Tool update(Tool tool) throws SQLException;
    Tool setActive(long toolId, boolean active) throws SQLException;

    Optional<Tool> findById(long toolId) throws SQLException;

    List<Tool> findAllByMachineId(long cncMachineId) throws SQLException;

    Optional<Tool> findByMachineIdAndToolNumber(long cncMachineId, int toolNumber) throws SQLException;
}
