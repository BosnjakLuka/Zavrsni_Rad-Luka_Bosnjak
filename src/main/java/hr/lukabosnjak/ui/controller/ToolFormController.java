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
    private Tool editingTool;

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
            Tool value = new Tool(
                    editingTool == null ? null : editingTool.getToolId(),
                    machine,
                    NumericInputParser.parseRequiredInteger(toolNumberInput.getText(), "Broj alata"),
                    nameInput.getText(),
                    typeInput.getText(),
                    NumericInputParser.parseRequiredFinite(diameterInput.getText(), "Promjer"),
                    NumericInputParser.parseRequiredFinite(cuttingLengthInput.getText(), "Rezna duljina"),
                    NumericInputParser.parseRequiredInteger(fluteCountInput.getText(), "Broj oštrica"),
                    true,
                    null,
                    null);
            savedTool = editingTool == null
                    ? managementService.createTool(value)
                    : managementService.updateTool(value);
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

    public void setTool(Tool tool) {
        editingTool = Objects.requireNonNull(tool);
        setMachine(tool.getCncMachine());
        toolNumberInput.setText(Integer.toString(tool.getToolNumber()));
        nameInput.setText(tool.getName());
        typeInput.setText(tool.getType());
        diameterInput.setText(Double.toString(tool.getDiameter()));
        cuttingLengthInput.setText(Double.toString(tool.getCuttingLength()));
        fluteCountInput.setText(Integer.toString(tool.getFluteCount()));
    }

    private void closeWindow() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}
