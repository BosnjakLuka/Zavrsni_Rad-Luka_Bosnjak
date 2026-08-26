package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.service.ReferenceDataAccessException;
import hr.lukabosnjak.service.ReferenceDataManagementService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Objects;

public final class CncMachineFormController {
    @FXML private TextField nameInput;
    @FXML private TextField manufacturerInput;
    @FXML private TextField modelInput;
    @FXML private TextField controllerInput;
    @FXML private TextField workAreaXInput;
    @FXML private TextField workAreaYInput;
    @FXML private TextField workAreaZInput;
    @FXML private TextField maxFeedRateInput;
    @FXML private TextField minSpindleSpeedInput;
    @FXML private TextField maxSpindleSpeedInput;
    @FXML private Label statusLabel;

    private final ReferenceDataManagementService managementService;
    private CncMachine savedMachine;
    private CncMachine editingMachine;

    public CncMachineFormController(ReferenceDataManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService);
    }

    @FXML
    private void handleSave() {
        try {
            CncMachine value = new CncMachine(
                    editingMachine == null ? null : editingMachine.getCncMachineId(),
                    nameInput.getText(),
                    manufacturerInput.getText(),
                    modelInput.getText(),
                    controllerInput.getText(),
                    numeric(workAreaXInput, "Radna površina X"),
                    numeric(workAreaYInput, "Radna površina Y"),
                    optionalNumeric(workAreaZInput, "Radna površina Z"),
                    optionalNumeric(maxFeedRateInput, "Maksimalni posmak"),
                    optionalNumeric(minSpindleSpeedInput, "Minimalna brzina vretena"),
                    optionalNumeric(maxSpindleSpeedInput, "Maksimalna brzina vretena"),
                    null,
                    null);
            savedMachine = editingMachine == null
                    ? managementService.createMachine(value)
                    : managementService.updateMachine(value);
            closeWindow();
        } catch (IllegalArgumentException | ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    public CncMachine getSavedMachine() {
        return savedMachine;
    }

    public void setMachine(CncMachine machine) {
        editingMachine = Objects.requireNonNull(machine);
        nameInput.setText(machine.getName());
        manufacturerInput.setText(machine.getManufacturer());
        modelInput.setText(machine.getModel());
        controllerInput.setText(machine.getController());
        workAreaXInput.setText(Double.toString(machine.getWorkAreaX()));
        workAreaYInput.setText(Double.toString(machine.getWorkAreaY()));
        setOptional(workAreaZInput, machine.getWorkAreaZ());
        setOptional(maxFeedRateInput, machine.getMaxFeedRate());
        setOptional(minSpindleSpeedInput, machine.getMinSpindleSpeed());
        setOptional(maxSpindleSpeedInput, machine.getMaxSpindleSpeed());
    }

    private double numeric(TextField input, String label) {
        return NumericInputParser.parseRequiredFinite(input.getText(), label);
    }

    private Double optionalNumeric(TextField input, String label) {
        return NumericInputParser.parseOptionalFinite(input.getText(), label);
    }

    private void closeWindow() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }

    private void setOptional(TextField input, Double value) {
        input.setText(value == null ? "" : Double.toString(value));
    }
}
