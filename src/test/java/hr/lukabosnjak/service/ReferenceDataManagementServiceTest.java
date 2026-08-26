package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.persistence.repository.CncMachineRepository;
import hr.lukabosnjak.persistence.repository.MaterialTypeRepository;
import hr.lukabosnjak.persistence.repository.ToolRepository;
import hr.lukabosnjak.validation.ReferenceDataValidator;
import hr.lukabosnjak.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceDataManagementServiceTest {

    @Test
    void createsNormalizedMaterialMachineAndToolReferences() {
        MaterialRepositoryStub materials = new MaterialRepositoryStub();
        MachineRepositoryStub machines = new MachineRepositoryStub();
        ToolRepositoryStub tools = new ToolRepositoryStub();
        ReferenceDataManagementService service = service(materials, machines, tools);

        MaterialType material = service.createMaterialType(
                new MaterialType(null, "  MDF  ", "   ", null, null, null));
        CncMachine machine = service.createMachine(machine("  ZK-1325  "));
        Tool tool = service.createTool(new Tool(
                null, machine, 1, "  Glodalo  ", "  Ravno  ", 6.0, 20.0, 2,
                true, null, null));

        assertEquals("MDF", material.getName());
        assertNull(material.getDescription());
        assertEquals("ZK-1325", machine.getName());
        assertEquals("Glodalo", tool.getName());
        assertEquals("Ravno", tool.getType());
        assertEquals(machine.getCncMachineId(), tool.getCncMachine().getCncMachineId());
    }

    @Test
    void rejectsCaseInsensitiveNamesAndDuplicateToolNumberPerMachine() {
        MaterialRepositoryStub materials = new MaterialRepositoryStub();
        MachineRepositoryStub machines = new MachineRepositoryStub();
        ToolRepositoryStub tools = new ToolRepositoryStub();
        ReferenceDataManagementService service = service(materials, machines, tools);

        service.createMaterialType(new MaterialType(null, "MDF", null, null, null, null));
        CncMachine machine = service.createMachine(machine("ZK-1325"));
        service.createTool(tool(machine, 1));

        assertThrows(ValidationException.class, () -> service.createMaterialType(
                new MaterialType(null, " mdf ", null, null, null, null)));
        assertThrows(ValidationException.class, () -> service.createMachine(machine(" zk-1325 ")));
        assertThrows(ValidationException.class, () -> service.createTool(tool(machine, 1)));
    }

    @Test
    void wrapsRepositoryFailures() {
        MaterialRepositoryStub materials = new MaterialRepositoryStub();
        materials.failure = new SQLException("database unavailable");
        ReferenceDataManagementService service = service(
                materials, new MachineRepositoryStub(), new ToolRepositoryStub());

        assertThrows(ReferenceDataAccessException.class, () -> service.createMaterialType(
                new MaterialType(null, "MDF", null, null, null, null)));
    }

    private ReferenceDataManagementService service(
            MaterialTypeRepository materials,
            CncMachineRepository machines,
            ToolRepository tools
    ) {
        SessionContext session = new SessionContext();
        session.login(new hr.lukabosnjak.domain.entities.User(
                1L, new hr.lukabosnjak.domain.entities.Role(1L, "ADMIN", null),
                "admin", "hash", "Admin", "User", true, null, null));
        return new ReferenceDataManagementService(materials, machines, tools,
                new ReferenceDataValidator(), new AuthorizationService(session));
    }

    private CncMachine machine(String name) {
        return new CncMachine(
                null, name, "Proizvođač", "Model", "RichAuto A11",
                1250.0, 2500.0, 80.0, 5000.0, 1000.0, 24000.0,
                null, null);
    }

    private Tool tool(CncMachine machine, int number) {
        return new Tool(
                null, machine, number, "Glodalo", "Ravno", 6.0, 20.0, 2,
                true, null, null);
    }

    private static final class MaterialRepositoryStub implements MaterialTypeRepository {
        private final List<MaterialType> values = new ArrayList<>();
        private SQLException failure;

        @Override
        public MaterialType save(MaterialType materialType) throws SQLException {
            failIfNeeded();
            materialType.setMaterialTypeId((long) values.size() + 1);
            values.add(materialType);
            return materialType;
        }

        @Override public Optional<MaterialType> findById(long id) { return values.stream()
                .filter(value -> value.getMaterialTypeId() == id).findFirst(); }
        @Override public List<MaterialType> findAll() throws SQLException { failIfNeeded(); return List.copyOf(values); }
        @Override public MaterialType update(MaterialType value) { return value; }
        private void failIfNeeded() throws SQLException { if (failure != null) throw failure; }
    }

    private static final class MachineRepositoryStub implements CncMachineRepository {
        private final List<CncMachine> values = new ArrayList<>();

        @Override
        public CncMachine save(CncMachine machine) {
            machine.setCncMachineId((long) values.size() + 1);
            values.add(machine);
            return machine;
        }

        @Override public Optional<CncMachine> findById(long id) { return values.stream()
                .filter(value -> value.getCncMachineId() == id).findFirst(); }
        @Override public List<CncMachine> findAll() { return List.copyOf(values); }
        @Override public CncMachine update(CncMachine value) { return value; }
    }

    private static final class ToolRepositoryStub implements ToolRepository {
        private final List<Tool> values = new ArrayList<>();

        @Override
        public Tool save(Tool tool) {
            tool.setToolId((long) values.size() + 1);
            values.add(tool);
            return tool;
        }

        @Override public Optional<Tool> findById(long id) { return values.stream()
                .filter(value -> value.getToolId() == id).findFirst(); }
        @Override public List<Tool> findAllByMachineId(long machineId) { return values.stream()
                .filter(value -> value.getCncMachine().getCncMachineId() == machineId).toList(); }
        @Override public Optional<Tool> findByMachineIdAndToolNumber(long machineId, int toolNumber) {
            return values.stream().filter(value -> value.getCncMachine().getCncMachineId() == machineId
                    && value.getToolNumber() == toolNumber).findFirst();
        }
        @Override public Tool update(Tool value) { return value; }
    }
}
