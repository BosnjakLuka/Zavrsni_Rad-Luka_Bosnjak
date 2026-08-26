package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.persistence.repository.CncMachineRepository;
import hr.lukabosnjak.persistence.repository.MaterialTypeRepository;
import hr.lukabosnjak.persistence.repository.ToolRepository;
import hr.lukabosnjak.validation.ReferenceDataValidator;
import hr.lukabosnjak.validation.ValidationException;

import java.sql.SQLException;
import java.util.Objects;

/** Creates selectable reference data without exposing JDBC or SQL to JavaFX controllers. */
public final class ReferenceDataManagementService {
    private final MaterialTypeRepository materialTypeRepository;
    private final CncMachineRepository machineRepository;
    private final ToolRepository toolRepository;
    private final ReferenceDataValidator validator;

    public ReferenceDataManagementService(
            MaterialTypeRepository materialTypeRepository,
            CncMachineRepository machineRepository,
            ToolRepository toolRepository,
            ReferenceDataValidator validator
    ) {
        this.materialTypeRepository = Objects.requireNonNull(materialTypeRepository);
        this.machineRepository = Objects.requireNonNull(machineRepository);
        this.toolRepository = Objects.requireNonNull(toolRepository);
        this.validator = Objects.requireNonNull(validator);
    }

    public MaterialType createMaterialType(MaterialType materialType) {
        Objects.requireNonNull(materialType, "materialType");
        normalize(materialType);
        validator.validate(materialType);
        try {
            boolean duplicate = materialTypeRepository.findAll().stream()
                    .map(MaterialType::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(name -> name.equalsIgnoreCase(materialType.getName()));
            if (duplicate) {
                throw new ValidationException("Vrsta materijala s tim nazivom već postoji.");
            }
            return materialTypeRepository.save(materialType);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Spremanje vrste materijala nije uspjelo.", exception);
        }
    }

    public CncMachine createMachine(CncMachine machine) {
        Objects.requireNonNull(machine, "machine");
        normalize(machine);
        validator.validate(machine);
        try {
            boolean duplicate = machineRepository.findAll().stream()
                    .map(CncMachine::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(name -> name.equalsIgnoreCase(machine.getName()));
            if (duplicate) {
                throw new ValidationException("CNC stroj s tim nazivom već postoji.");
            }
            return machineRepository.save(machine);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Spremanje CNC stroja nije uspjelo.", exception);
        }
    }

    public Tool createTool(Tool tool) {
        Objects.requireNonNull(tool, "tool");
        normalize(tool);
        validator.validate(tool);
        long machineId = tool.getCncMachine().getCncMachineId();
        try {
            if (toolRepository.findByMachineIdAndToolNumber(machineId, tool.getToolNumber()).isPresent()) {
                throw new ValidationException("Odabrani CNC stroj već ima alat s tim brojem.");
            }
            return toolRepository.save(tool);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Spremanje alata nije uspjelo.", exception);
        }
    }

    private void normalize(MaterialType materialType) {
        materialType.setName(trim(materialType.getName()));
        materialType.setDescription(trimToNull(materialType.getDescription()));
    }

    private void normalize(CncMachine machine) {
        machine.setName(trim(machine.getName()));
        machine.setManufacturer(trimToNull(machine.getManufacturer()));
        machine.setModel(trimToNull(machine.getModel()));
        machine.setController(trim(machine.getController()));
    }

    private void normalize(Tool tool) {
        tool.setName(trim(tool.getName()));
        tool.setType(trim(tool.getType()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
