package hr.lukabosnjak.persistence.jdbc;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.service.SavedJobService;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcMachiningJobRepositoryIntegrationTest {
    private static final String TEST_G_CODE = "G21\nG90\nM30\n";

    private ConnectionProvider connectionProvider;

    @BeforeEach
    void initializeDatabase() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:machining-job-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
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
    void roundTripsCompleteSingleElementJobFromFreshRepositoryContext() throws Exception {
        References references = saveReferences();
        JdbcMachiningJobRepository savingRepository = new JdbcMachiningJobRepository(connectionProvider);

        SavedJobService savingService = new SavedJobService(savingRepository);
        MachiningJob saved = savingService.save(newJob(references, 1));

        JdbcMachiningJobRepository freshRepository = new JdbcMachiningJobRepository(connectionProvider);
        MachiningJob loaded = freshRepository.findById(saved.getMachiningJobId()).orElseThrow();
        SavedJobService freshSavedJobService = new SavedJobService(freshRepository);
        MachiningJob quickAccessLoaded = freshSavedJobService.loadById(saved.getMachiningJobId());

        assertNotNull(loaded.getMachiningJobId());
        assertEquals("Single element test job", loaded.getName());
        assertEquals(1, loaded.getQuantity());
        assertEquals(TEST_G_CODE, loaded.getGCode());
        assertEquals(TEST_G_CODE, freshSavedJobService.gCodeProgramOf(quickAccessLoaded).text());
        assertEquals(10.0, quickAccessLoaded.getMachiningParameters().getSafeZ());
        assertEquals(80.0, quickAccessLoaded.getShape().getDimensionA());
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getUpdatedAt());

        assertEquals(references.user().getUserId(), loaded.getCreatedBy().getUserId());
        assertEquals("OPERATOR", loaded.getCreatedBy().getRole().getName());
        assertEquals(references.machine().getCncMachineId(), loaded.getCncMachine().getCncMachineId());
        assertEquals(references.tool().getToolId(), loaded.getTool().getToolId());
        assertEquals(references.machine().getCncMachineId(),
                loaded.getTool().getCncMachine().getCncMachineId());
        assertNull(loaded.getCncMachine().getWorkAreaZ());
        assertNull(loaded.getCncMachine().getMaxFeedRate());
        assertNull(loaded.getCncMachine().getMinSpindleSpeed());
        assertNull(loaded.getCncMachine().getMaxSpindleSpeed());
        assertNull(loaded.getTool().getCncMachine().getWorkAreaZ());

        assertNotNull(loaded.getMaterialSheet().getMaterialSheetId());
        assertEquals(references.materialType().getMaterialTypeId(),
                loaded.getMaterialSheet().getMaterialType().getMaterialTypeId());
        assertEquals(18.0, loaded.getMaterialSheet().getThickness());
        assertNotNull(loaded.getMachiningParameters().getMachiningParametersId());
        assertEquals(10.0, loaded.getMachiningParameters().getSafeZ());
        assertNotNull(loaded.getShape().getShapeId());
        assertEquals(ShapeType.CIRCLE, loaded.getShape().getShapeType());
        assertEquals(80.0, loaded.getShape().getDimensionA());
        assertNull(loaded.getShape().getDimensionB());

        assertEquals(1, freshRepository.findAll().size());
        assertEquals(saved.getMachiningJobId(), freshRepository.findAll().getFirst().getMachiningJobId());
        assertFalse(freshRepository.findById(Long.MAX_VALUE).isPresent());
    }

    @Test
    void rollsBackAllOwnedSnapshotsWhenJobInsertFails() throws Exception {
        References references = saveReferences();
        JdbcMachiningJobRepository repository = new JdbcMachiningJobRepository(connectionProvider);

        assertThrows(SQLException.class, () -> repository.save(newJob(references, 0)));

        assertEquals(0, countRows("MACHINING_JOB"));
        assertEquals(0, countRows("MATERIAL_SHEET"));
        assertEquals(0, countRows("MACHINING_PARAMETERS"));
        assertEquals(0, countRows("SHAPE"));
        assertEquals(1, countRows("MATERIAL_TYPE"));
        assertEquals(1, countRows("CNC_MACHINE"));
        assertEquals(1, countRows("TOOL"));
        assertEquals(1, countRows("APP_USER"));
    }

    private References saveReferences() throws Exception {
        MaterialType materialType = new JdbcMaterialTypeRepository(connectionProvider).save(
                new MaterialType(null, "Test material", null, null, null, null));
        CncMachine machine = new JdbcCncMachineRepository(connectionProvider).save(new CncMachine(
                null, "ZK-1325", null, "ZK-1325", "RichAuto A11",
                1250.0, 2500.0, null, null, null, null, null, null));
        Tool tool = new JdbcToolRepository(connectionProvider).save(new Tool(
                null, machine, 1, "Test tool", "TEST_TYPE", 6.0, 20.0, 2,
                true, null, null));
        JdbcRoleRepository roleRepository = new JdbcRoleRepository(connectionProvider);
        Role role = roleRepository.save(new Role(null, "OPERATOR", null));
        User user = new JdbcUserRepository(connectionProvider).save(new User(
                null, role, "job-repository-user", "test-hash", "Test", "User",
                true, null, null));
        return new References(materialType, machine, tool, user);
    }

    private MachiningJob newJob(References references, int quantity) {
        MaterialSheet sheet = new MaterialSheet(
                null, references.materialType(), 600.0, 400.0, 18.0, null, null);
        MachiningParameters parameters = new MachiningParameters(
                null, 18000.0, 2400.0, 600.0, 6.0, 3.0, 10.0);
        Shape shape = new Shape(
                null, ShapeType.CIRCLE, null, 80.0, null, null, null, null, null);
        return new MachiningJob(
                null, references.user(), references.machine(), references.tool(), sheet,
                parameters, shape, "Single element test job", quantity, TEST_G_CODE, null, null);
    }

    private int countRows(String tableName) throws SQLException {
        String sql = switch (tableName) {
            case "MACHINING_JOB" -> "SELECT COUNT(*) FROM MACHINING_JOB";
            case "MATERIAL_SHEET" -> "SELECT COUNT(*) FROM MATERIAL_SHEET";
            case "MACHINING_PARAMETERS" -> "SELECT COUNT(*) FROM MACHINING_PARAMETERS";
            case "SHAPE" -> "SELECT COUNT(*) FROM SHAPE";
            case "MATERIAL_TYPE" -> "SELECT COUNT(*) FROM MATERIAL_TYPE";
            case "CNC_MACHINE" -> "SELECT COUNT(*) FROM CNC_MACHINE";
            case "TOOL" -> "SELECT COUNT(*) FROM TOOL";
            case "APP_USER" -> "SELECT COUNT(*) FROM APP_USER";
            default -> throw new IllegalArgumentException("Unsupported table: " + tableName);
        };
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private record References(MaterialType materialType, CncMachine machine, Tool tool, User user) {
    }
}
