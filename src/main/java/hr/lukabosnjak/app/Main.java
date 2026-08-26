package hr.lukabosnjak.app;

import hr.lukabosnjak.config.DatabaseInitializer;
import hr.lukabosnjak.gcode.NcExportService;
import hr.lukabosnjak.gcode.RichAutoA11GCodeGenerator;
import hr.lukabosnjak.gcode.RichAutoA11Profile;
import hr.lukabosnjak.geometry.ToolPathBoundsCalculator;
import hr.lukabosnjak.geometry.ToolPathService;
import hr.lukabosnjak.persistence.jdbc.JdbcCncMachineRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcMachiningJobRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcMaterialTypeRepository;
import hr.lukabosnjak.persistence.jdbc.JdbcToolRepository;
import hr.lukabosnjak.service.MaterialReferenceDataService;
import hr.lukabosnjak.service.ProgramExportService;
import hr.lukabosnjak.service.ProgramGenerationService;
import hr.lukabosnjak.service.ReferenceDataService;
import hr.lukabosnjak.service.SavedJobService;
import hr.lukabosnjak.ui.controller.MainFormController;
import hr.lukabosnjak.validation.MachiningJobValidator;
import hr.lukabosnjak.validation.MachiningParametersValidator;
import hr.lukabosnjak.validation.MaterialSheetValidator;
import hr.lukabosnjak.validation.ShapeValidator;
import hr.lukabosnjak.validation.SingleShapeFitValidator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Set;

import static hr.lukabosnjak.gcode.RichAutoA11Profile.ArcCenterMode.RELATIVE_TO_ARC_START;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.PositioningMode.ABSOLUTE;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.Units.MILLIMETERS;
import static hr.lukabosnjak.gcode.RichAutoA11Profile.ZCoordinateConvention.MATERIAL_SURFACE_ZERO_NEGATIVE_CUT;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        DatabaseInitializer.initialize();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/hr/lukabosnjak/ui/view/main-form.fxml"));
        loader.setControllerFactory(controllerType -> {
            if (controllerType == MainFormController.class) {
                return new MainFormController(
                        programGenerationService(), referenceDataService(), materialReferenceDataService(),
                        savedJobService(), programExportService());
            }
            throw new IllegalArgumentException("Unsupported controller: " + controllerType.getName());
        });
        Scene scene = new Scene(loader.load(), 1100, 760);
        stage.setTitle("CNC Optimizer");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    private ProgramGenerationService programGenerationService() {
        ShapeValidator shapeValidator = new ShapeValidator();
        MaterialSheetValidator materialSheetValidator = new MaterialSheetValidator();
        return new ProgramGenerationService(
                new MachiningJobValidator(shapeValidator, materialSheetValidator, new MachiningParametersValidator()),
                new ToolPathService(shapeValidator),
                new SingleShapeFitValidator(new ToolPathBoundsCalculator(), materialSheetValidator),
                new RichAutoA11GCodeGenerator(conservativePreviewProfile()));
    }

    private ReferenceDataService referenceDataService() {
        return new ReferenceDataService(new JdbcCncMachineRepository(), new JdbcToolRepository());
    }

    private MaterialReferenceDataService materialReferenceDataService() {
        return new MaterialReferenceDataService(new JdbcMaterialTypeRepository());
    }

    private SavedJobService savedJobService() {
        return new SavedJobService(new JdbcMachiningJobRepository());
    }

    private ProgramExportService programExportService() {
        return new ProgramExportService(new NcExportService());
    }

    private RichAutoA11Profile conservativePreviewProfile() {
        return new RichAutoA11Profile(
                false, false, false, false,
                MILLIMETERS, ABSOLUTE, 3,
                MATERIAL_SURFACE_ZERO_NEGATIVE_CUT, RELATIVE_TO_ARC_START, Set.of());
    }
}
