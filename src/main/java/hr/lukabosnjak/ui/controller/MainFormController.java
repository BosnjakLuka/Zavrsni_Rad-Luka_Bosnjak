package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.enums.ShapeType;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class MainFormController {
    @FXML
    private ComboBox<ShapeType> shapeTypeComboBox;
    @FXML
    private VBox dynamicShapeFields;
    @FXML
    private TextField materialWidthInput;
    @FXML
    private TextField materialHeightInput;
    @FXML
    private TextField materialThicknessInput;
    @FXML
    private TextField spindleSpeedInput;
    @FXML
    private TextField feedRateInput;
    @FXML
    private TextField plungeRateInput;
    @FXML
    private TextField cutDepthInput;
    @FXML
    private TextField stepDownInput;
    @FXML
    private TextField safeZInput;
    @FXML
    private TextArea gCodePreview;
    @FXML
    private Label statusLabel;

    private final List<ShapeInput> shapeInputs = new ArrayList<>();

    @FXML
    private void initialize() {
        shapeTypeComboBox.getItems().setAll(ShapeType.values());
        shapeTypeComboBox.setValue(ShapeType.SQUARE);
        shapeTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateShapeFields(newValue));
        updateShapeFields(shapeTypeComboBox.getValue());
        statusLabel.setText("Forma je spremna. Generiranje, spremanje i izvoz još nisu povezani.");
    }

    @FXML
    private void handleGenerate() {
        try {
            parseVisibleNumericInputs();
            statusLabel.setText("Unos je numerički ispravan. Generiranje G-codea još nije integrirano.");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        statusLabel.setText("Spremanje programa još nije povezano s persistence slojem.");
    }

    @FXML
    private void handleExport() {
        statusLabel.setText("Izvoz .nc datoteke još nije povezan s G-code programom.");
    }

    @FXML
    private void handleSavedPrograms() {
        statusLabel.setText("Prikaz spremljenih programa još nije povezan s persistence slojem.");
    }

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

    private void parseVisibleNumericInputs() {
        for (ShapeInput shapeInput : shapeInputs) {
            NumericInputParser.parseRequiredFinite(shapeInput.input().getText(), shapeInput.label());
        }
        NumericInputParser.parseRequiredFinite(materialWidthInput.getText(), "Širina ploče");
        NumericInputParser.parseRequiredFinite(materialHeightInput.getText(), "Visina ploče");
        NumericInputParser.parseRequiredFinite(materialThicknessInput.getText(), "Debljina ploče");
        NumericInputParser.parseRequiredFinite(spindleSpeedInput.getText(), "Brzina vretena");
        NumericInputParser.parseRequiredFinite(feedRateInput.getText(), "Brzina posmaka");
        NumericInputParser.parseRequiredFinite(plungeRateInput.getText(), "Brzina uranjanja");
        NumericInputParser.parseRequiredFinite(cutDepthInput.getText(), "Dubina reza");
        NumericInputParser.parseRequiredFinite(stepDownInput.getText(), "Step-down");
        NumericInputParser.parseRequiredFinite(safeZInput.getText(), "Safe Z");
    }

    private record ShapeInput(String label, TextField input) {
    }
}
