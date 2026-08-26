package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.Tool;
import hr.lukabosnjak.service.ReferenceDataAccessException;
import hr.lukabosnjak.service.ReferenceDataManagementService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Objects;

public final class ToolFormController {
    @FXML private Label machineLabel;
    @FXML private TextField toolNumberInput;
    @FXML private TextField nameInput;
    @FXML private TextField typeInput;
    @FXML private TextField diameterInput;
    @FXML private TextField cuttingLengthInput;
    @FXML private TextField fluteCountInput;
    @FXML private Label statusLabel;

    private final ReferenceDataManagementService managementService;
    private CncMachine machine;
    private Tool savedTool;

    public ToolFormController(ReferenceDataManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService);
    }

    public void setMachine(CncMachine machine) {
        this.machine = Objects.requireNonNull(machine);
        machineLabel.setText(machine.getName());
    }

    @FXML
    private void handleSave() {
        try {
            savedTool = managementService.createTool(new Tool(
                    null,
                    machine,
                    NumericInputParser.parseRequiredInteger(toolNumberInput.getText(), "Broj alata"),
                    nameInput.getText(),
                    typeInput.getText(),
                    NumericInputParser.parseRequiredFinite(diameterInput.getText(), "Promjer"),
                    NumericInputParser.parseRequiredFinite(cuttingLengthInput.getText(), "Rezna duljina"),
                    NumericInputParser.parseRequiredInteger(fluteCountInput.getText(), "Broj oštrica"),
                    true,
                    null,
                    null));
            closeWindow();
        } catch (IllegalArgumentException | ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    public Tool getSavedTool() {
        return savedTool;
    }

    private void closeWindow() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}
