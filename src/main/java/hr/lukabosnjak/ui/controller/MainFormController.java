package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.service.MaterialReferenceDataService;
import hr.lukabosnjak.service.ProgramExportService;
import hr.lukabosnjak.service.ProgramGenerationRequest;
import hr.lukabosnjak.service.ProgramGenerationService;
import hr.lukabosnjak.service.ReferenceDataAccessException;
import hr.lukabosnjak.service.ReferenceDataService;
import hr.lukabosnjak.service.SavedJobAccessException;
import hr.lukabosnjak.service.SavedJobService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainFormController {
    @FXML private ComboBox<ShapeType> shapeTypeComboBox;
    @FXML private ComboBox<CncMachine> machineComboBox;
    @FXML private ComboBox<Tool> toolComboBox;
    @FXML private ComboBox<MaterialType> materialTypeComboBox;
    @FXML private TextField jobNameInput;
    @FXML private VBox dynamicShapeFields;
    @FXML private TextField materialWidthInput;
    @FXML private TextField materialHeightInput;
    @FXML private TextField materialThicknessInput;
    @FXML private TextField spindleSpeedInput;
    @FXML private TextField feedRateInput;
    @FXML private TextField plungeRateInput;
    @FXML private TextField cutDepthInput;
    @FXML private TextField stepDownInput;
    @FXML private TextField safeZInput;
    @FXML private TextArea gCodePreview;
    @FXML private ListView<MachiningJob> savedJobsList;
    @FXML private Label statusLabel;

    private final ProgramGenerationService programGenerationService;
    private final ReferenceDataService referenceDataService;
    private final MaterialReferenceDataService materialReferenceDataService;
    private final SavedJobService savedJobService;
    private final ProgramExportService programExportService;
    private final List<ShapeInput> shapeInputs = new ArrayList<>();
    private GCodeProgram displayedProgram;

    public MainFormController(
            ProgramGenerationService programGenerationService,
            ReferenceDataService referenceDataService,
            MaterialReferenceDataService materialReferenceDataService,
            SavedJobService savedJobService,
            ProgramExportService programExportService
    ) {
        this.programGenerationService = programGenerationService;
        this.referenceDataService = referenceDataService;
        this.materialReferenceDataService = materialReferenceDataService;
        this.savedJobService = savedJobService;
        this.programExportService = programExportService;
    }

    @FXML
    private void initialize() {
        shapeTypeComboBox.getItems().setAll(ShapeType.values());
        shapeTypeComboBox.setValue(ShapeType.SQUARE);
        shapeTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateShapeFields(newValue));
        machineComboBox.setConverter(machineConverter());
        toolComboBox.setConverter(toolConverter());
        materialTypeComboBox.setConverter(materialTypeConverter());
        savedJobsList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(MachiningJob job, boolean empty) {
                super.updateItem(job, empty);
                setText(empty || job == null ? null : "#" + job.getMachiningJobId() + " — " + job.getName());
            }
        });
        updateShapeFields(shapeTypeComboBox.getValue());
        loadReferenceData();
    }

    @FXML
    private void handleGenerate() {
        try {
            displayedProgram = programGenerationService.generate(readGenerationRequest());
            gCodePreview.setText(displayedProgram.text());
            statusLabel.setText("G-code za jedan element uspješno je generiran. Nije fizički testiran na stroju.");
        } catch (IllegalArgumentException | ReferenceDataAccessException exception) {
            clearPreview();
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleMachineChanged() {
        loadToolsForSelectedMachine();
    }

    @FXML
    private void handleSave() {
        statusLabel.setText("Spremanje zahtijeva prijavljenu sesiju autora, koja još nije implementirana.");
    }

    @FXML
    private void handleExport() {
        if (displayedProgram == null) {
            statusLabel.setText("Najprije generirajte ili otvorite program za export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Izvoz CNC programa");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CNC program (*.nc)", "*.nc"));
        chooser.setInitialFileName(suggestedNcFileName());
        File file = chooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (file == null) {
            statusLabel.setText("Izvoz je otkazan.");
            return;
        }
        try {
            programExportService.export(displayedProgram, file.toPath());
            statusLabel.setText("Program je izvezen u .nc datoteku.");
        } catch (IOException | IllegalArgumentException exception) {
            statusLabel.setText("Izvoz nije uspio: " + exception.getMessage());
        }
    }

    @FXML
    private void handleSavedPrograms() {
        try {
            savedJobsList.getItems().setAll(savedJobService.loadAll());
            statusLabel.setText(savedJobsList.getItems().isEmpty()
                    ? "Nema spremljenih programa."
                    : "Odaberite spremljeni program i otvorite ga.");
        } catch (SavedJobAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleOpenSavedJob() {
        MachiningJob selectedJob = savedJobsList.getSelectionModel().getSelectedItem();
        if (selectedJob == null) {
            statusLabel.setText("Odaberite spremljeni program za otvaranje.");
            return;
        }
        try {
            populateForm(savedJobService.loadById(selectedJob.getMachiningJobId()));
            statusLabel.setText("Spremljeni program je učitan u formu.");
        } catch (IllegalArgumentException | SavedJobAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private void loadReferenceData() {
        try {
            machineComboBox.getItems().setAll(referenceDataService.loadMachines());
            materialTypeComboBox.getItems().setAll(materialReferenceDataService.loadMaterialTypes());
            statusLabel.setText("Odaberite stroj, alat i vrstu materijala te unesite podatke za jedan element.");
        } catch (ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private void loadToolsForSelectedMachine() {
        toolComboBox.getItems().clear();
        toolComboBox.setValue(null);
        try {
            toolComboBox.getItems().setAll(referenceDataService.loadTools(machineComboBox.getValue()));
        } catch (ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private ProgramGenerationRequest readGenerationRequest() {
        ShapeType shapeType = shapeTypeComboBox.getValue();
        if (shapeType == null) throw new IllegalArgumentException("Vrsta oblika mora biti odabrana.");
        MaterialType materialType = materialTypeComboBox.getValue();
        if (materialType == null) throw new IllegalArgumentException("Vrsta materijala mora biti odabrana.");
        List<Double> dimensions = shapeInputs.stream()
                .map(input -> NumericInputParser.parseRequiredFinite(input.input().getText(), input.label()))
                .toList();
        Shape shape = new Shape(null, shapeType, shapeType == ShapeType.TRIANGLE ? ShapeSubtype.EQUILATERAL : null,
                dimensions.getFirst(), dimensions.size() > 1 ? dimensions.get(1) : null, null, null, null, null);
        MaterialSheet sheet = new MaterialSheet(null, materialType,
                numeric(materialWidthInput, "Širina ploče"), numeric(materialHeightInput, "Visina ploče"),
                numeric(materialThicknessInput, "Debljina ploče"), null, null);
        MachiningParameters parameters = new MachiningParameters(null,
                numeric(spindleSpeedInput, "Brzina vretena"), numeric(feedRateInput, "Brzina posmaka"),
                numeric(plungeRateInput, "Brzina uranjanja"), numeric(cutDepthInput, "Dubina reza"),
                numeric(stepDownInput, "Step-down"), numeric(safeZInput, "Safe Z"));
        return new ProgramGenerationRequest(machineComboBox.getValue(), toolComboBox.getValue(), sheet, parameters, shape);
    }

    private void populateForm(MachiningJob job) {
        jobNameInput.setText(job.getName());
        materialTypeComboBox.setValue(job.getMaterialSheet().getMaterialType());
        machineComboBox.setValue(job.getCncMachine());
        loadToolsForSelectedMachine();
        toolComboBox.setValue(job.getTool());
        materialWidthInput.setText(Double.toString(job.getMaterialSheet().getWidth()));
        materialHeightInput.setText(Double.toString(job.getMaterialSheet().getHeight()));
        materialThicknessInput.setText(Double.toString(job.getMaterialSheet().getThickness()));
        spindleSpeedInput.setText(Double.toString(job.getMachiningParameters().getSpindleSpeed()));
        feedRateInput.setText(Double.toString(job.getMachiningParameters().getFeedRate()));
        plungeRateInput.setText(Double.toString(job.getMachiningParameters().getPlungeRate()));
        cutDepthInput.setText(Double.toString(job.getMachiningParameters().getCutDepth()));
        stepDownInput.setText(Double.toString(job.getMachiningParameters().getStepDown()));
        safeZInput.setText(Double.toString(job.getMachiningParameters().getSafeZ()));
        shapeTypeComboBox.setValue(job.getShape().getShapeType());
        shapeInputs.getFirst().input().setText(Double.toString(job.getShape().getDimensionA()));
        if (shapeInputs.size() > 1) shapeInputs.get(1).input().setText(Double.toString(job.getShape().getDimensionB()));
        displayedProgram = savedJobService.gCodeProgramOf(job);
        gCodePreview.setText(displayedProgram.text());
    }

    private void updateShapeFields(ShapeType shapeType) {
        shapeInputs.clear();
        dynamicShapeFields.getChildren().clear();
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(8);
        switch (shapeType) {
            case SQUARE -> addShapeInput(fields, 0, "Stranica (mm)");
            case RECTANGLE -> { addShapeInput(fields, 0, "Širina (mm)"); addShapeInput(fields, 1, "Visina (mm)"); }
            case CIRCLE -> addShapeInput(fields, 0, "Promjer (mm)");
            case TRIANGLE -> addShapeInput(fields, 0, "Stranica (mm)");
        }
        dynamicShapeFields.getChildren().add(fields);
    }

    private void addShapeInput(GridPane fields, int row, String label) {
        TextField input = new TextField();
        input.setPromptText("0.0");
        fields.add(new Label(label), 0, row);
        fields.add(input, 1, row);
        shapeInputs.add(new ShapeInput(label, input));
    }

    private double numeric(TextField input, String label) { return NumericInputParser.parseRequiredFinite(input.getText(), label); }
    private void clearPreview() { displayedProgram = null; gCodePreview.clear(); }
    private String suggestedNcFileName() { return jobNameInput.getText().isBlank() ? "cnc-program.nc" : jobNameInput.getText() + ".nc"; }

    private StringConverter<CncMachine> machineConverter() { return converter(CncMachine::getName); }
    private StringConverter<MaterialType> materialTypeConverter() { return converter(MaterialType::getName); }
    private StringConverter<Tool> toolConverter() { return converter(tool -> "T" + tool.getToolNumber() + " — " + tool.getName()); }
    private <T> StringConverter<T> converter(java.util.function.Function<T, String> display) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : display.apply(value); }
            @Override public T fromString(String value) { throw new UnsupportedOperationException("Selection is not editable"); }
        };
    }

    private record ShapeInput(String label, TextField input) { }
}
