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
import java.util.List;
import java.util.Objects;

/** Manages reference data without exposing JDBC or SQL to JavaFX controllers. */
public final class ReferenceDataManagementService {
    private final MaterialTypeRepository materialTypeRepository;
    private final CncMachineRepository machineRepository;
    private final ToolRepository toolRepository;
    private final ReferenceDataValidator validator;
    private final AuthorizationService authorizationService;

    public ReferenceDataManagementService(
            MaterialTypeRepository materialTypeRepository,
            CncMachineRepository machineRepository,
            ToolRepository toolRepository,
            ReferenceDataValidator validator,
            AuthorizationService authorizationService
    ) {
        this.materialTypeRepository = Objects.requireNonNull(materialTypeRepository);
        this.machineRepository = Objects.requireNonNull(machineRepository);
        this.toolRepository = Objects.requireNonNull(toolRepository);
        this.validator = Objects.requireNonNull(validator);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public MaterialType createMaterialType(MaterialType materialType) {
        requireCatalogAccess();
        Objects.requireNonNull(materialType, "materialType");
        normalize(materialType);
        validator.validate(materialType);
        try {
            boolean duplicate = materialTypeRepository.findAll().stream()
                    .map(MaterialType::getName).filter(Objects::nonNull)
                    .anyMatch(name -> name.equalsIgnoreCase(materialType.getName()));
            if (duplicate) throw new ValidationException("Vrsta materijala s tim nazivom već postoji.");
            return materialTypeRepository.save(materialType);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Spremanje vrste materijala nije uspjelo.", exception);
        }
    }

    public CncMachine createMachine(CncMachine machine) {
        requireCatalogAccess();
        Objects.requireNonNull(machine, "machine");
        normalize(machine);
        validator.validate(machine);
        try {
            boolean duplicate = machineRepository.findAll().stream()
                    .map(CncMachine::getName).filter(Objects::nonNull)
                    .anyMatch(name -> name.equalsIgnoreCase(machine.getName()));
            if (duplicate) throw new ValidationException("CNC stroj s tim nazivom već postoji.");
            return machineRepository.save(machine);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Spremanje CNC stroja nije uspjelo.", exception);
        }
    }

    public Tool createTool(Tool tool) {
        requireCatalogAccess();
        Objects.requireNonNull(tool, "tool");
        normalize(tool);
        validator.validate(tool);
        try {
            if (toolRepository.findByMachineIdAndToolNumber(
                    tool.getCncMachine().getCncMachineId(), tool.getToolNumber()).isPresent()) {
                throw new ValidationException("Odabrani CNC stroj već ima alat s tim brojem.");
            }
            return toolRepository.save(tool);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Spremanje alata nije uspjelo.", exception);
        }
    }

    public List<MaterialType> loadMaterialTypes() {
        requireCatalogAccess();
        try { return materialTypeRepository.findAll(); }
        catch (SQLException exception) {
            throw new ReferenceDataAccessException("Dohvat vrsta materijala nije uspio.", exception);
        }
    }

    public List<CncMachine> loadMachines() {
        requireCatalogAccess();
        try { return machineRepository.findAll(); }
        catch (SQLException exception) {
            throw new ReferenceDataAccessException("Dohvat CNC strojeva nije uspio.", exception);
        }
    }

    public List<Tool> loadTools(CncMachine machine) {
        requireCatalogAccess();
        if (machine == null || machine.getCncMachineId() == null) {
            throw new IllegalArgumentException("CNC stroj mora biti odabran.");
        }
        try { return toolRepository.findAllByMachineId(machine.getCncMachineId()); }
        catch (SQLException exception) {
            throw new ReferenceDataAccessException("Dohvat alata nije uspio.", exception);
        }
    }

    public MaterialType updateMaterialType(MaterialType materialType) {
        requireCatalogAccess();
        Objects.requireNonNull(materialType, "materialType");
        normalize(materialType);
        validator.validate(materialType);
        try { return materialTypeRepository.update(materialType); }
        catch (SQLException exception) {
            throw new ReferenceDataAccessException("Uređivanje vrste materijala nije uspjelo.", exception);
        }
    }

    public CncMachine updateMachine(CncMachine machine) {
        requireCatalogAccess();
        Objects.requireNonNull(machine, "machine");
        normalize(machine);
        validator.validate(machine);
        try { return machineRepository.update(machine); }
        catch (SQLException exception) {
            throw new ReferenceDataAccessException("Uređivanje CNC stroja nije uspjelo.", exception);
        }
    }

    public Tool updateTool(Tool tool) {
        requireCatalogAccess();
        Objects.requireNonNull(tool, "tool");
        normalize(tool);
        validator.validate(tool);
        try { return toolRepository.update(tool); }
        catch (SQLException exception) {
            throw new ReferenceDataAccessException("Uređivanje alata nije uspjelo.", exception);
        }
    }

    public MaterialType setMaterialTypeActive(long id, boolean active) {
        requireCatalogAccess();
        try {
            return materialTypeRepository.setActive(id, active);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Promjena statusa vrste materijala nije uspjela.", exception);
        }
    }

    public CncMachine setMachineActive(long id, boolean active) {
        requireCatalogAccess();
        try {
            return machineRepository.setActive(id, active);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Promjena statusa CNC stroja nije uspjela.", exception);
        }
    }

    public Tool setToolActive(long id, boolean active) {
        requireCatalogAccess();
        try {
            return toolRepository.setActive(id, active);
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Promjena statusa alata nije uspjela.", exception);
        }
    }

    private void requireCatalogAccess() {
        authorizationService.require(AuthorizationService.Permission.MANAGE_REFERENCE_DATA);
    }

    private void normalize(MaterialType value) {
        value.setName(trim(value.getName()));
        value.setDescription(trimToNull(value.getDescription()));
    }

    private void normalize(CncMachine value) {
        value.setName(trim(value.getName()));
        value.setManufacturer(trimToNull(value.getManufacturer()));
        value.setModel(trimToNull(value.getModel()));
        value.setController(trim(value.getController()));
    }

    private void normalize(Tool value) {
        value.setName(trim(value.getName()));
        value.setType(trim(value.getType()));
    }

    private String trim(String value) { return value == null ? null : value.trim(); }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
