package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRepositoriesIntegrationTest {
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void initializeDatabase() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:repositories-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        connectionProvider = () -> DriverManager.getConnection(jdbcUrl, "sa", "");

        try (Connection connection = connectionProvider.getConnection()) {
            InputStream stream = getClass().getResourceAsStream("/db/schema.sql");
            assertNotNull(stream);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                RunScript.execute(connection, reader);
            }
        }
    }

    @Test
    void roundTripsReferenceRepositoriesAndMapsNullableColumns() throws Exception {
        JdbcMaterialTypeRepository materialTypes = new JdbcMaterialTypeRepository(connectionProvider);
        JdbcCncMachineRepository machines = new JdbcCncMachineRepository(connectionProvider);

        MaterialType material = materialTypes.save(new MaterialType(
                null, "Plywood", null, null, null, null));
        CncMachine machine = machines.save(machine("Machine A"));

        assertNotNull(material.getMaterialTypeId());
        assertNull(material.getDescription());
        assertNull(material.getDeletedAt());
        assertNotNull(material.getCreatedAt());
        assertEquals("Plywood", materialTypes.findById(material.getMaterialTypeId()).orElseThrow().getName());
        assertEquals(List.of(material.getMaterialTypeId()), materialTypes.findAll().stream()
                .map(MaterialType::getMaterialTypeId).toList());

        CncMachine loadedMachine = machines.findById(machine.getCncMachineId()).orElseThrow();
        assertEquals("Machine A", loadedMachine.getName());
        assertNull(loadedMachine.getManufacturer());
        assertEquals(1250.0, loadedMachine.getWorkAreaX());
        assertEquals(1, machines.findAll().size());
    }

    @Test
    void roundTripsSnapshotRepositoriesAndMapsShapeEnums() throws Exception {
        JdbcMaterialTypeRepository materialTypes = new JdbcMaterialTypeRepository(connectionProvider);
        JdbcMaterialSheetRepository sheets = new JdbcMaterialSheetRepository(connectionProvider);
        JdbcMachiningParametersRepository parameters = new JdbcMachiningParametersRepository(connectionProvider);
        JdbcShapeRepository shapes = new JdbcShapeRepository(connectionProvider);

        MaterialType material = materialTypes.save(new MaterialType(
                null, "MDF", "Test material", null, null, null));
        MaterialSheet sheet = sheets.save(new MaterialSheet(
                null, material, 800.0, 600.0, 18.0, null, null));
        MachiningParameters savedParameters = parameters.save(new MachiningParameters(
                null, 18000.0, 2400.0, 600.0, 6.0, 3.0, 10.0));
        Shape shape = shapes.save(new Shape(
                null, ShapeType.TRIANGLE, ShapeSubtype.EQUILATERAL,
                120.0, null, null, null, null, null));

        MaterialSheet loadedSheet = sheets.findById(sheet.getMaterialSheetId()).orElseThrow();
        assertEquals(material.getMaterialTypeId(), loadedSheet.getMaterialType().getMaterialTypeId());
        assertEquals("MDF", loadedSheet.getMaterialType().getName());
        assertEquals(18.0, loadedSheet.getThickness());

        MachiningParameters loadedParameters = parameters
                .findById(savedParameters.getMachiningParametersId()).orElseThrow();
        assertEquals(600.0, loadedParameters.getPlungeRate());
        assertEquals(10.0, loadedParameters.getSafeZ());

        Shape loadedShape = shapes.findById(shape.getShapeId()).orElseThrow();
        assertEquals(ShapeType.TRIANGLE, loadedShape.getShapeType());
        assertEquals(ShapeSubtype.EQUILATERAL, loadedShape.getShapeSubtype());
        assertNull(loadedShape.getDimensionB());
        assertNull(loadedShape.getDimensionC());
    }

    @Test
    void mapsSeededRoleAndRoundTripsMinimalUserData() throws Exception {
        JdbcRoleRepository roles = new JdbcRoleRepository(connectionProvider);
        JdbcUserRepository users = new JdbcUserRepository(connectionProvider);

        Role role = roles.findByName("USER").orElseThrow();
        assertNull(role.getDescription());

        User saved = users.save(new User(
                null, role, "repository-test-user", "test-hash", "Test", "User",
                true, null, null));
        User loaded = users.findByUsername("repository-test-user").orElseThrow();

        assertNotNull(saved.getUserId());
        assertEquals(saved.getUserId(), loaded.getUserId());
        assertEquals("USER", loaded.getRole().getName());
        assertEquals("test-hash", loaded.getPasswordHash());
        assertTrue(loaded.isActive());
        assertFalse(users.findByUsername("missing-user").isPresent());
    }

    @Test
    void enforcesToolNumberUniquenessPerMachineAndSupportsMachineQueries() throws Exception {
        JdbcCncMachineRepository machines = new JdbcCncMachineRepository(connectionProvider);
        JdbcToolRepository tools = new JdbcToolRepository(connectionProvider);
        CncMachine firstMachine = machines.save(machine("Machine A"));
        CncMachine secondMachine = machines.save(machine("Machine B"));

        Tool firstTool = tools.save(tool(firstMachine, 1, "First tool"));
        SQLException duplicate = assertThrows(SQLException.class,
                () -> tools.save(tool(firstMachine, 1, "Duplicate tool")));
        assertNotNull(duplicate.getSQLState());

        Tool otherMachineTool = tools.save(tool(secondMachine, 1, "Other machine tool"));
        Tool secondTool = tools.save(tool(firstMachine, 2, "Second tool"));

        assertNotNull(firstTool.getToolId());
        assertNotNull(otherMachineTool.getToolId());
        assertEquals(List.of(1, 2), tools.findAllByMachineId(firstMachine.getCncMachineId()).stream()
                .map(Tool::getToolNumber).toList());
        assertEquals(secondTool.getToolId(), tools.findByMachineIdAndToolNumber(
                firstMachine.getCncMachineId(), 2).orElseThrow().getToolId());
        assertEquals("Machine A", tools.findById(firstTool.getToolId()).orElseThrow()
                .getCncMachine().getName());
        assertTrue(tools.findByMachineIdAndToolNumber(firstMachine.getCncMachineId(), 99).isEmpty());
    }

    private CncMachine machine(String name) {
        return new CncMachine(
                null, name, null, null, "Test controller", 1250.0, 2500.0, 150.0,
                5000.0, 1000.0, 24000.0, null, null);
    }

    private Tool tool(CncMachine machine, int toolNumber, String name) {
        return new Tool(
                null, machine, toolNumber, name, "TEST_TYPE", 6.0, 20.0, 2,
                true, LocalDateTime.now(), LocalDateTime.now());
    }
}
