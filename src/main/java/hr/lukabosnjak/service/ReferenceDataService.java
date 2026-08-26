package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.persistence.repository.CncMachineRepository;
import hr.lukabosnjak.persistence.repository.ToolRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Reads selectable reference data for the UI; it neither creates nor changes persisted data. */
public final class ReferenceDataService {
    private final CncMachineRepository machineRepository;
    private final ToolRepository toolRepository;

    public ReferenceDataService(CncMachineRepository machineRepository, ToolRepository toolRepository) {
        this.machineRepository = Objects.requireNonNull(machineRepository);
        this.toolRepository = Objects.requireNonNull(toolRepository);
    }

    public List<CncMachine> loadMachines() {
        try {
            return machineRepository.findAll();
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Učitavanje CNC strojeva nije uspjelo.", exception);
        }
    }

    public List<Tool> loadTools(CncMachine machine) {
        if (machine == null || machine.getCncMachineId() == null) {
            return List.of();
        }
        try {
            return toolRepository.findAllByMachineId(machine.getCncMachineId());
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Učitavanje alata nije uspjelo.", exception);
        }
    }
}
