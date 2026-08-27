package hr.lukabosnjak.app;

import hr.lukabosnjak.config.DatabaseConfig;
import hr.lukabosnjak.config.DatabaseInitializer;
import hr.lukabosnjak.gcode.NcExportService;
import hr.lukabosnjak.gcode.RichAutoA11GCodeGenerator;
import hr.lukabosnjak.gcode.RichAutoA11Profile;
import hr.lukabosnjak.geometry.ToolPathBoundsCalculator;
import hr.lukabosnjak.geometry.ToolPathCompensationService;
import hr.lukabosnjak.geometry.ToolPathService;
import hr.lukabosnjak.persistence.jdbc.ConnectionProvider;
import hr.lukabosnjak.persistence.jdbc.JdbcCncMachineRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcMachiningJobRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcMaterialTypeRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcRoleRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcToolRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcUserRepository;
import hr.lukabosnjak.persistence.repository.CncMachineRepository;
import hr.lukabosnjak.persistence.repository.MachiningJobRepository;
import hr.lukabosnjak.persistence.repository.MaterialTypeRepository;
import hr.lukabosnjak.persistence.repository.RoleRepository;
import hr.lukabosnjak.persistence.repository.ToolRepository;
import hr.lukabosnjak.persistence.repository.UserRepository;
import hr.lukabosnjak.service.MaterialReferenceDataService;
import hr.lukabosnjak.service.MachiningParametersPreset;
import hr.lukabosnjak.service.AuthService;
import hr.lukabosnjak.service.AuthorizationService;
import hr.lukabosnjak.service.PasswordHasher;
import hr.lukabosnjak.service.ProgramExportService;
import hr.lukabosnjak.service.ProgramGenerationService;
import hr.lukabosnjak.service.ReferenceDataService;
import hr.lukabosnjak.service.ReferenceDataManagementService;
import hr.lukabosnjak.service.SavedJobService;
import hr.lukabosnjak.service.SessionContext;
import hr.lukabosnjak.service.V1BootstrapService;
import hr.lukabosnjak.service.UserManagementService;
import hr.lukabosnjak.ui.controller.CncMachineFormController;
import hr.lukabosnjak.ui.controller.ApplicationNavigation;
import hr.lukabosnjak.ui.controller.LoginController;
import hr.lukabosnjak.ui.controller.MainFormController;
import hr.lukabosnjak.ui.controller.MaterialTypeFormController;
import hr.lukabosnjak.ui.controller.RegistrationController;
import hr.lukabosnjak.ui.controller.ToolFormController;
import hr.lukabosnjak.ui.controller.UserManagementController;
import hr.lukabosnjak.ui.controller.SavedProgramsController;
import hr.lukabosnjak.ui.controller.CatalogController;
import hr.lukabosnjak.validation.MachiningJobValidator;
import hr.lukabosnjak.validation.MachiningParametersValidator;
import hr.lukabosnjak.validation.MaterialSheetValidator;
import hr.lukabosnjak.validation.ReferenceDataValidator;
import hr.lukabosnjak.validation.ShapeValidator;
import hr.lukabosnjak.validation.SingleShapeFitValidator;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

import static hr.lukabosnjak.gcode.RichAutoA11Profile.ArcCenterMode.RELATIVE_TO_ARC_START;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.PositioningMode.ABSOLUTE;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.Units.MILLIMETERS;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.ZCoordinateConvention.MATERIAL_SURFACE_ZERO_NEGATIVE_CUT;

/** The single manual composition root for the non-modular JavaFX application. */
final class ApplicationCompositionRoot {
    private final ConnectionProvider connectionProvider;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MaterialTypeRepository materialTypeRepository;
    private final CncMachineRepository cncMachineRepository;
    private final ToolRepository toolRepository;
    private final MachiningJobRepository machiningJobRepository;

    private final ProgramGenerationService programGenerationService;
    private final MachiningParametersPreset machiningParametersPreset;
    private final ReferenceDataService referenceDataService;
    private final MaterialReferenceDataService materialReferenceDataService;
    private final ReferenceDataManagementService referenceDataManagementService;
    private final SavedJobService savedJobService;
    private final ProgramExportService programExportService;
    private final V1BootstrapService v1BootstrapService;
    private final AuthService authService;
    private final SessionContext sessionContext;
    private final AuthorizationService authorizationService;
    private final UserManagementService userManagementService;

    private ApplicationCompositionRoot(ConnectionProvider connectionProvider) {
        Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.connectionProvider = connectionProvider;

        roleRepository = new JdbcRoleRepository(connectionProvider);
        userRepository = new JdbcUserRepository(connectionProvider);
        materialTypeRepository = new JdbcMaterialTypeRepository(connectionProvider);
        cncMachineRepository = new JdbcCncMachineRepository(connectionProvider);
        toolRepository = new JdbcToolRepository(connectionProvider);
        machiningJobRepository = new JdbcMachiningJobRepository(connectionProvider);

        ShapeValidator shapeValidator = new ShapeValidator();
        MaterialSheetValidator materialSheetValidator = new MaterialSheetValidator();
        MachiningParametersValidator machiningParametersValidator = new MachiningParametersValidator();
        MachiningJobValidator machiningJobValidator = new MachiningJobValidator(
                shapeValidator, materialSheetValidator, machiningParametersValidator);
        ToolPathService toolPathService = new ToolPathService(shapeValidator);
        SingleShapeFitValidator singleShapeFitValidator = new SingleShapeFitValidator(
                new ToolPathBoundsCalculator(), materialSheetValidator);
        RichAutoA11GCodeGenerator gCodeGenerator = new RichAutoA11GCodeGenerator(referencePreviewProfile());

        programGenerationService = new ProgramGenerationService(
                machiningJobValidator, toolPathService, singleShapeFitValidator,
                new ToolPathCompensationService(), gCodeGenerator);
        machiningParametersPreset = MachiningParametersPreset.referenceDefaults();
        referenceDataService = new ReferenceDataService(cncMachineRepository, toolRepository);
        materialReferenceDataService = new MaterialReferenceDataService(materialTypeRepository);
        savedJobService = new SavedJobService(machiningJobRepository);
        programExportService = new ProgramExportService(new NcExportService());
        PasswordHasher passwordHasher = new PasswordHasher();
        sessionContext = new SessionContext();
        authService = new AuthService(userRepository, roleRepository, passwordHasher, sessionContext);
        authorizationService = new AuthorizationService(sessionContext);
        referenceDataManagementService = new ReferenceDataManagementService(
                materialTypeRepository, cncMachineRepository, toolRepository,
                new ReferenceDataValidator(), authorizationService);
        userManagementService = new UserManagementService(
                userRepository, roleRepository, sessionContext, authorizationService);
        v1BootstrapService = new V1BootstrapService(
                roleRepository, materialTypeRepository, cncMachineRepository, userRepository,
                toolRepository);
    }

    static ApplicationCompositionRoot production() {
        return new ApplicationCompositionRoot(DatabaseConfig::getConnection);
    }

    static ApplicationCompositionRoot forConnectionProvider(ConnectionProvider connectionProvider) {
        return new ApplicationCompositionRoot(connectionProvider);
    }

    void initializeDatabase() throws SQLException {
        try (java.sql.Connection connection = connectionProvider.getConnection()) {
            DatabaseInitializer.initialize(connection);
        }
        v1BootstrapService.initialize();
    }

    Object createController(Class<?> controllerType, ApplicationNavigation navigation) {
        Objects.requireNonNull(navigation, "navigation");
        if (controllerType == LoginController.class) {
            return new LoginController(authService, navigation);
        }
        if (controllerType == RegistrationController.class) {
            return new RegistrationController(authService, navigation);
        }
        if (controllerType == MainFormController.class) {
            return new MainFormController(
                    programGenerationService, referenceDataService, materialReferenceDataService,
                    machiningParametersPreset,
                    savedJobService, programExportService, authService, sessionContext, authorizationService, navigation,
                    type -> createController(type, navigation));
        }
        if (controllerType == UserManagementController.class) {
            return new UserManagementController(userManagementService, navigation);
        }
        if (controllerType == SavedProgramsController.class) {
            return new SavedProgramsController(
                    savedJobService, programExportService, sessionContext, navigation);
        }
        if (controllerType == CatalogController.class) {
            return new CatalogController(
                    referenceDataManagementService, savedJobService, programExportService,
                    authorizationService, sessionContext,
                    navigation, type -> createController(type, navigation));
        }
        if (controllerType == MaterialTypeFormController.class) {
            return new MaterialTypeFormController(referenceDataManagementService);
        }
        if (controllerType == CncMachineFormController.class) {
            return new CncMachineFormController(referenceDataManagementService);
        }
        if (controllerType == ToolFormController.class) {
            return new ToolFormController(referenceDataManagementService);
        }
        throw new IllegalArgumentException("Unsupported controller: " + controllerType.getName());
    }

    ProgramGenerationService programGenerationService() {
        return programGenerationService;
    }

    RoleRepository roleRepository() {
        return roleRepository;
    }

    UserRepository userRepository() {
        return userRepository;
    }

    MaterialTypeRepository materialTypeRepository() {
        return materialTypeRepository;
    }

    CncMachineRepository cncMachineRepository() {
        return cncMachineRepository;
    }

    ToolRepository toolRepository() {
        return toolRepository;
    }

    MachiningJobRepository machiningJobRepository() {
        return machiningJobRepository;
    }

    AuthService authService() {
        return authService;
    }

    SessionContext sessionContext() {
        return sessionContext;
    }

    AuthorizationService authorizationService() {
        return authorizationService;
    }

    private RichAutoA11Profile referencePreviewProfile() {
        return RichAutoA11Profile.referenceProgramProfile();
    }
}
