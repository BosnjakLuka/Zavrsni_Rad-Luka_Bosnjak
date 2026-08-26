package hr.lukabosnjak.app;

import hr.lukabosnjak.config.DatabaseInitializer;
import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.persistence.jdbc.ConnectionProvider;
import hr.lukabosnjak.service.ProgramGenerationRequest;
import hr.lukabosnjak.ui.controller.CncMachineFormController;
import hr.lukabosnjak.ui.controller.ApplicationNavigation;
import hr.lukabosnjak.ui.controller.LoginController;
import hr.lukabosnjak.ui.controller.MainFormController;
import hr.lukabosnjak.ui.controller.MaterialTypeFormController;
import hr.lukabosnjak.ui.controller.RegistrationController;
import hr.lukabosnjak.ui.controller.ToolFormController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationCompositionRootIntegrationTest {
    private static final double SOFTWARE_TEST_SPINDLE_SPEED = 12_000.0;
    private static final double SOFTWARE_TEST_FEED_RATE = 500.0;
    private static final double SOFTWARE_TEST_PLUNGE_RATE = 200.0;
    private static final double SOFTWARE_TEST_CUT_DEPTH = 3.0;
    private static final double SOFTWARE_TEST_STEP_DOWN = 1.0;
    private static final double SOFTWARE_TEST_SAFE_Z = 5.0;

    private ConnectionProvider connectionProvider;

    @BeforeEach
    void initializeFreshDatabase() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:composition-root-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        connectionProvider = () -> DriverManager.getConnection(jdbcUrl, "sa", "");
        try (Connection connection = connectionProvider.getConnection()) {
            DatabaseInitializer.initialize(connection);
        }
    }

    @Test
    void generatesPersistsAndReloadsSingleEquilateralTriangleFromFreshContext() throws Exception {
        ApplicationCompositionRoot firstContext = ApplicationCompositionRoot.forConnectionProvider(connectionProvider);
        firstContext.initializeDatabase();
        PersistedReferences references = saveReferences(firstContext);

        MaterialSheet sheet = new MaterialSheet(
                null, references.materialType(), 500.0, 500.0, 18.0, null, null);
        MachiningParameters parameters = new MachiningParameters(
                null, SOFTWARE_TEST_SPINDLE_SPEED, SOFTWARE_TEST_FEED_RATE,
                SOFTWARE_TEST_PLUNGE_RATE, SOFTWARE_TEST_CUT_DEPTH,
                SOFTWARE_TEST_STEP_DOWN, SOFTWARE_TEST_SAFE_Z);
        Shape shape = new Shape(
                null, ShapeType.TRIANGLE, ShapeSubtype.EQUILATERAL,
                30.0, null, null, null, null, null);
        ProgramGenerationRequest input = new ProgramGenerationRequest(
                references.machine(), references.tool(), sheet, parameters, shape);

        GCodeProgram previewResult = firstContext.programGenerationService().generate(input);
        assertTrue(previewResult.text().startsWith("G21\nG17\nG90\n"));
        assertTrue(previewResult.text().endsWith("M30\n"));

        MachiningJob saved = firstContext.machiningJobRepository().save(new MachiningJob(
                null, references.user(), references.machine(), references.tool(), sheet,
                parameters, shape, "Software E2E triangle", 1, previewResult.text(), null, null));

        ApplicationCompositionRoot freshContext = ApplicationCompositionRoot.forConnectionProvider(connectionProvider);
        MachiningJob reloaded = freshContext.machiningJobRepository()
                .findById(saved.getMachiningJobId()).orElseThrow();

        assertNotNull(reloaded.getMachiningJobId());
        assertEquals(ShapeType.TRIANGLE, reloaded.getShape().getShapeType());
        assertEquals(ShapeSubtype.EQUILATERAL, reloaded.getShape().getShapeSubtype());
        assertEquals(30.0, reloaded.getShape().getDimensionA());
        assertEquals(references.materialType().getMaterialTypeId(),
                reloaded.getMaterialSheet().getMaterialType().getMaterialTypeId());
        assertEquals(500.0, reloaded.getMaterialSheet().getWidth());
        assertEquals(500.0, reloaded.getMaterialSheet().getHeight());
        assertEquals(18.0, reloaded.getMaterialSheet().getThickness());
        assertEquals(SOFTWARE_TEST_SPINDLE_SPEED, reloaded.getMachiningParameters().getSpindleSpeed());
        assertEquals(SOFTWARE_TEST_FEED_RATE, reloaded.getMachiningParameters().getFeedRate());
        assertEquals(SOFTWARE_TEST_PLUNGE_RATE, reloaded.getMachiningParameters().getPlungeRate());
        assertEquals(SOFTWARE_TEST_CUT_DEPTH, reloaded.getMachiningParameters().getCutDepth());
        assertEquals(SOFTWARE_TEST_STEP_DOWN, reloaded.getMachiningParameters().getStepDown());
        assertEquals(SOFTWARE_TEST_SAFE_Z, reloaded.getMachiningParameters().getSafeZ());
        assertEquals(1, reloaded.getQuantity());
        assertEquals(previewResult.text(), reloaded.getGCode());
        assertEquals(references.machine().getCncMachineId(), reloaded.getCncMachine().getCncMachineId());
        assertEquals(references.tool().getToolId(), reloaded.getTool().getToolId());
        assertEquals(references.user().getUserId(), reloaded.getCreatedBy().getUserId());
    }

    @Test
    void createsMainAndReferenceDataFormControllersFromOneCompositionRoot() {
        ApplicationCompositionRoot context = ApplicationCompositionRoot.forConnectionProvider(connectionProvider);
        ApplicationNavigation navigation = new NoOpNavigation();

        assertInstanceOf(LoginController.class, context.createController(LoginController.class, navigation));
        assertInstanceOf(RegistrationController.class,
                context.createController(RegistrationController.class, navigation));
        assertInstanceOf(MainFormController.class, context.createController(MainFormController.class, navigation));
        assertInstanceOf(MaterialTypeFormController.class,
                context.createController(MaterialTypeFormController.class, navigation));
        assertInstanceOf(CncMachineFormController.class,
                context.createController(CncMachineFormController.class, navigation));
        assertInstanceOf(ToolFormController.class, context.createController(ToolFormController.class, navigation));
    }

    private PersistedReferences saveReferences(ApplicationCompositionRoot context) throws Exception {
        MaterialType materialType = context.materialTypeRepository().save(
                new MaterialType(null, "Software test material", null, null, null, null));
        CncMachine machine = context.cncMachineRepository().save(new CncMachine(
                null, "Software test machine", null, null, "Software test controller",
                1250.0, 2500.0, 150.0, 5000.0, 1000.0, 24000.0, null, null));
        Tool tool = context.toolRepository().save(new Tool(
                null, machine, 1, "Software test tool", "SOFTWARE_TEST_TYPE",
                6.0, 20.0, 2, true, null, null));
        Role role = context.roleRepository().findByName("OPERATOR").orElseThrow();
        User user = context.userRepository().save(new User(
                null, role, "software-e2e-user", "software-test-hash",
                "Software", "Test", true, null, null));
        return new PersistedReferences(materialType, machine, tool, user);
    }

    private record PersistedReferences(MaterialType materialType, CncMachine machine, Tool tool, User user) {
    }

    private static final class NoOpNavigation implements ApplicationNavigation {
        @Override public void showLogin() { }
        @Override public void showRegistration() { }
        @Override public void showUserManagement() { }
        @Override public void showSavedPrograms() { }
        @Override public void showCatalog() { }
        @Override public void showMain() { }
    }
}
