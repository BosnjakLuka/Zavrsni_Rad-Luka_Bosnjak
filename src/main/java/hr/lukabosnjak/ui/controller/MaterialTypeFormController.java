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
    private MaterialType editingMaterialType;

    public MaterialTypeFormController(ReferenceDataManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService);
    }

    @FXML
    private void handleSave() {
        try {
            MaterialType value = new MaterialType(
                    editingMaterialType == null ? null : editingMaterialType.getMaterialTypeId(),
                    nameInput.getText(), descriptionInput.getText(), null, null, null);
            savedMaterialType = editingMaterialType == null
                    ? managementService.createMaterialType(value)
                    : managementService.updateMaterialType(value);
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

    public void setMaterialType(MaterialType materialType) {
        editingMaterialType = Objects.requireNonNull(materialType);
        nameInput.setText(materialType.getName());
        descriptionInput.setText(materialType.getDescription());
    }

    private void closeWindow() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}
