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

    public CncMachineFormController(ReferenceDataManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService);
    }

    @FXML
    private void handleSave() {
        try {
            savedMachine = managementService.createMachine(new CncMachine(
                    null,
                    nameInput.getText(),
                    manufacturerInput.getText(),
                    modelInput.getText(),
                    controllerInput.getText(),
                    numeric(workAreaXInput, "Radna površina X"),
                    numeric(workAreaYInput, "Radna površina Y"),
                    numeric(workAreaZInput, "Radna površina Z"),
                    numeric(maxFeedRateInput, "Maksimalni posmak"),
                    numeric(minSpindleSpeedInput, "Minimalna brzina vretena"),
                    numeric(maxSpindleSpeedInput, "Maksimalna brzina vretena"),
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

    public CncMachine getSavedMachine() {
        return savedMachine;
    }

    private double numeric(TextField input, String label) {
        return NumericInputParser.parseRequiredFinite(input.getText(), label);
    }

    private void closeWindow() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}
