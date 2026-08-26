package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.domain.enums.ShapeSubtype;
import hr.lukabosnjak.domain.enums.ShapeType;
import hr.lukabosnjak.service.ProgramGenerationRequest;
import hr.lukabosnjak.service.ProgramGenerationService;
import hr.lukabosnjak.service.ReferenceDataAccessException;
import hr.lukabosnjak.service.ReferenceDataService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class MainFormController {
    @FXML private ComboBox<ShapeType> shapeTypeComboBox;
    @FXML private ComboBox<CncMachine> machineComboBox;
    @FXML private ComboBox<Tool> toolComboBox;
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
    @FXML private Label statusLabel;

    private final ProgramGenerationService programGenerationService;
    private final ReferenceDataService referenceDataService;
    private final List<ShapeInput> shapeInputs = new ArrayList<>();

    public MainFormController(ProgramGenerationService programGenerationService, ReferenceDataService referenceDataService) {
        this.programGenerationService = programGenerationService;
        this.referenceDataService = referenceDataService;
    }

    @FXML
    private void initialize() {
        shapeTypeComboBox.getItems().setAll(ShapeType.values());
        shapeTypeComboBox.setValue(ShapeType.SQUARE);
        shapeTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateShapeFields(newValue));
        machineComboBox.setConverter(machineConverter());
        toolComboBox.setConverter(toolConverter());
        updateShapeFields(shapeTypeComboBox.getValue());
        loadMachines();
    }

    @FXML
    private void handleGenerate() {
        try {
            var program = programGenerationService.generate(readGenerationRequest());
            gCodePreview.setText(program.text());
            statusLabel.setText("G-code za jedan element uspješno je generiran. Nije fizički testiran na stroju.");
        } catch (IllegalArgumentException | ReferenceDataAccessException exception) {
            gCodePreview.clear();
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleMachineChanged() {
        toolComboBox.getItems().clear();
        toolComboBox.setValue(null);
        try {
            toolComboBox.getItems().setAll(referenceDataService.loadTools(machineComboBox.getValue()));
            if (machineComboBox.getValue() != null && toolComboBox.getItems().isEmpty()) {
                statusLabel.setText("Za odabrani CNC stroj nema dostupnih alata.");
            }
        } catch (ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML private void handleSave() { statusLabel.setText("Spremanje programa još nije povezano s persistence slojem."); }
    @FXML private void handleExport() { statusLabel.setText("Izvoz .nc datoteke još nije povezan s G-code programom."); }
    @FXML private void handleSavedPrograms() { statusLabel.setText("Prikaz spremljenih programa još nije povezan s persistence slojem."); }

    private void updateShapeFields(ShapeType shapeType) {
        shapeInputs.clear();
        dynamicShapeFields.getChildren().clear();
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(8);
        switch (shapeType) {
            case SQUARE -> addShapeInput(fields, 0, "Stranica (mm)");
            case RECTANGLE -> {
                addShapeInput(fields, 0, "Širina (mm)");
                addShapeInput(fields, 1, "Visina (mm)");
            }
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

    private void loadMachines() {
        try {
            machineComboBox.getItems().setAll(referenceDataService.loadMachines());
            statusLabel.setText(machineComboBox.getItems().isEmpty()
                    ? "Nema učitanih CNC strojeva. Generiranje zahtijeva spremljeni stroj i alat."
                    : "Odaberite CNC stroj i alat te unesite podatke za jedan element.");
        } catch (ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private ProgramGenerationRequest readGenerationRequest() {
        ShapeType shapeType = shapeTypeComboBox.getValue();
        if (shapeType == null) {
            throw new IllegalArgumentException("Vrsta oblika mora biti odabrana.");
        }
        List<Double> dimensions = shapeInputs.stream()
                .map(input -> NumericInputParser.parseRequiredFinite(input.input().getText(), input.label()))
                .toList();
        Shape shape = new Shape(null, shapeType, shapeType == ShapeType.TRIANGLE ? ShapeSubtype.EQUILATERAL : null,
                dimensions.getFirst(), dimensions.size() > 1 ? dimensions.get(1) : null, null, null, null, null);
        MaterialSheet sheet = new MaterialSheet(null, null,
                numeric(materialWidthInput, "Širina ploče"), numeric(materialHeightInput, "Visina ploče"),
                numeric(materialThicknessInput, "Debljina ploče"), null, null);
        MachiningParameters parameters = new MachiningParameters(null,
                numeric(spindleSpeedInput, "Brzina vretena"), numeric(feedRateInput, "Brzina posmaka"),
                numeric(plungeRateInput, "Brzina uranjanja"), numeric(cutDepthInput, "Dubina reza"),
                numeric(stepDownInput, "Step-down"), numeric(safeZInput, "Safe Z"));
        return new ProgramGenerationRequest(machineComboBox.getValue(), toolComboBox.getValue(), sheet, parameters, shape);
    }

    private double numeric(TextField input, String label) { return NumericInputParser.parseRequiredFinite(input.getText(), label); }

    private StringConverter<CncMachine> machineConverter() {
        return new StringConverter<>() {
            @Override public String toString(CncMachine machine) { return machine == null ? "" : machine.getName(); }
            @Override public CncMachine fromString(String value) { throw new UnsupportedOperationException("Machine selection is not editable"); }
        };
    }

    private StringConverter<Tool> toolConverter() {
        return new StringConverter<>() {
            @Override public String toString(Tool tool) { return tool == null ? "" : "T" + tool.getToolNumber() + " — " + tool.getName(); }
            @Override public Tool fromString(String value) { throw new UnsupportedOperationException("Tool selection is not editable"); }
        };
    }

    private record ShapeInput(String label, TextField input) { }
}
