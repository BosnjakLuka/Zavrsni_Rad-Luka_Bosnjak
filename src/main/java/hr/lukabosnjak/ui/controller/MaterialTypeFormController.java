package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.service.ReferenceDataAccessException;
import hr.lukabosnjak.service.ReferenceDataManagementService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Objects;

public final class MaterialTypeFormController {
    @FXML private TextField nameInput;
    @FXML private TextArea descriptionInput;
    @FXML private Label statusLabel;

    private final ReferenceDataManagementService managementService;
    private MaterialType savedMaterialType;

    public MaterialTypeFormController(ReferenceDataManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService);
    }

    @FXML
    private void handleSave() {
        try {
            savedMaterialType = managementService.createMaterialType(new MaterialType(
                    null, nameInput.getText(), descriptionInput.getText(), null, null, null));
            closeWindow();
        } catch (IllegalArgumentException | ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    public MaterialType getSavedMaterialType() {
        return savedMaterialType;
    }

    private void closeWindow() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}
