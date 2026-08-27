package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.service.AuthorizationService;
import hr.lukabosnjak.service.AuthorizationException;
import hr.lukabosnjak.service.ProgramExportService;
import hr.lukabosnjak.service.ReferenceDataAccessException;
import hr.lukabosnjak.service.ReferenceDataManagementService;
import hr.lukabosnjak.service.SavedJobAccessException;
import hr.lukabosnjak.service.SavedJobService;
import hr.lukabosnjak.service.SessionContext;
import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Tool;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.IOException;
import java.io.File;
import java.util.function.Consumer;

public final class CatalogController {
    @FXML private Button addMaterialTypeButton;
    @FXML private Button addMachineButton;
    @FXML private Button addToolButton;
    @FXML private Button deactivateMaterialTypeButton;
    @FXML private Button deactivateMachineButton;
    @FXML private Button deactivateToolButton;
    @FXML private Label currentUserLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<MaterialType> materialTypesList;
    @FXML private ListView<CncMachine> machinesList;
    @FXML private ComboBox<CncMachine> toolMachineComboBox;
    @FXML private ListView<Tool> toolsList;
    @FXML private ListView<MachiningJob> savedProgramsList;
    @FXML private TextArea savedProgramPreview;

    private final ReferenceDataManagementService managementService;
    private final SavedJobService savedJobService;
    private final ProgramExportService programExportService;
    private final AuthorizationService authorizationService;
    private final SessionContext sessionContext;
    private final ApplicationNavigation navigation;
    private final Callback<Class<?>, Object> controllerFactory;

    public CatalogController(ReferenceDataManagementService managementService,
                             SavedJobService savedJobService, ProgramExportService programExportService,
                             AuthorizationService authorizationService, SessionContext sessionContext,
                             ApplicationNavigation navigation, Callback<Class<?>, Object> controllerFactory) {
        this.managementService = managementService;
        this.savedJobService = savedJobService;
        this.programExportService = programExportService;
        this.authorizationService = authorizationService;
        this.sessionContext = sessionContext;
        this.navigation = navigation;
        this.controllerFactory = controllerFactory;
    }

    @FXML
    private void initialize() {
        currentUserLabel.setText(sessionContext.currentUser()
                .map(user -> user.getUsername() + " (" + user.getRole().getName() + ")")
                .orElse("Nema prijavljenog korisnika"));
        boolean allowed = authorizationService.isAllowed(
                AuthorizationService.Permission.MANAGE_REFERENCE_DATA);
        addMaterialTypeButton.setDisable(!allowed);
        addMachineButton.setDisable(!allowed);
        addToolButton.setDisable(!allowed);
        deactivateMaterialTypeButton.setDisable(!allowed);
        deactivateMachineButton.setDisable(!allowed);
        deactivateToolButton.setDisable(!allowed);
        statusLabel.setText("Odaberite katalog koji želite urediti.");
        materialTypesList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(MaterialType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName()
                        + (item.getDeletedAt() == null ? " — aktivno" : " — deaktivirano"));
            }
        });
        machinesList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(CncMachine item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " — " + item.getController()
                        + (item.isActive() ? " — aktivno" : " — deaktivirano"));
            }
        });
        toolsList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Tool item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "T" + item.getToolNumber()
                        + " — Ø" + item.getDiameter() + " mm — " + item.getName()
                        + (item.isActive() ? " — aktivno" : " — deaktivirano"));
            }
        });
        toolMachineComboBox.setConverter(machineConverter());
        savedProgramsList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(MachiningJob job, boolean empty) {
                super.updateItem(job, empty);
                setText(empty || job == null ? null : "#" + job.getMachiningJobId() + " — " + job.getName()
                        + " | " + job.getCreatedAt() + " | " + job.getCreatedBy().getUsername()
                        + " | " + job.getShape().getShapeType() + " | " + job.getCncMachine().getName());
            }
        });
        savedProgramsList.getSelectionModel().selectedItemProperty().addListener((obs, old, job) -> {
            savedProgramPreview.setText(job == null ? "" : savedJobService.gCodeProgramOf(job).text());
        });
        refresh();
    }

    @FXML private void handleAddMaterialType() {
        showDialog("/hr/lukabosnjak/ui/view/material-type-form.fxml", "Dodavanje vrste materijala",
                MaterialTypeFormController.class, ignored -> { });
    }

    @FXML private void handleAddMachine() {
        showDialog("/hr/lukabosnjak/ui/view/cnc-machine-form.fxml", "Dodavanje CNC stroja",
                CncMachineFormController.class, ignored -> { });
    }

    @FXML private void handleAddTool() {
        CncMachine machine = toolMachineComboBox.getValue();
        if (machine == null) {
            statusLabel.setText("Za dodavanje alata prvo odaberite CNC stroj.");
            return;
        }
        showDialog("/hr/lukabosnjak/ui/view/tool-form.fxml", "Dodavanje alata", ToolFormController.class,
                form -> form.setMachine(machine));
    }

    @FXML private void handleEditMaterialType() {
        MaterialType selected = materialTypesList.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Odaberite vrstu materijala."); return; }
        showDialog("/hr/lukabosnjak/ui/view/material-type-form.fxml", "Uređivanje vrste materijala",
                MaterialTypeFormController.class, form -> form.setMaterialType(selected));
    }

    @FXML private void handleEditMachine() {
        CncMachine selected = machinesList.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Odaberite CNC stroj."); return; }
        showDialog("/hr/lukabosnjak/ui/view/cnc-machine-form.fxml", "Uređivanje CNC stroja",
                CncMachineFormController.class, form -> form.setMachine(selected));
    }

    @FXML private void handleEditTool() {
        Tool selected = toolsList.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Odaberite alat."); return; }
        showDialog("/hr/lukabosnjak/ui/view/tool-form.fxml", "Uređivanje alata",
                ToolFormController.class, form -> form.setTool(selected));
    }

    @FXML private void handleRefresh() {
        refresh();
    }

    @FXML private void handleToolMachineChanged() {
        loadToolsForSelectedMachine();
    }

    @FXML private void handleOpenCatalogProgram() {
        MachiningJob selected = savedProgramsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Odaberite spremljeni program.");
            return;
        }
        try {
            authorizationService.require(AuthorizationService.Permission.MANAGE_REFERENCE_DATA);
            navigation.showMainWithJob(savedJobService.loadById(selected.getMachiningJobId()));
        } catch (AuthorizationException | IllegalArgumentException | SavedJobAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML private void handleExportCatalogProgram() {
        MachiningJob selected = savedProgramsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Odaberite spremljeni program.");
            return;
        }
        try {
            authorizationService.require(AuthorizationService.Permission.MANAGE_REFERENCE_DATA);
        } catch (AuthorizationException exception) {
            statusLabel.setText(exception.getMessage());
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Izvoz CNC programa");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CNC program (*.nc)", "*.nc"));
        chooser.setInitialFileName(selected.getName() + ".nc");
        File file = chooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            programExportService.export(savedJobService.gCodeProgramOf(selected), file.toPath());
            statusLabel.setText("Program je izvezen u .nc datoteku.");
        } catch (IOException | IllegalArgumentException exception) {
            statusLabel.setText("Izvoz nije uspio: " + exception.getMessage());
        }
    }

    @FXML private void handleToggleMaterialType() {
        MaterialType selected = materialTypesList.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Odaberite vrstu materijala."); return; }
        try {
            managementService.setMaterialTypeActive(selected.getMaterialTypeId(), selected.getDeletedAt() != null);
            refresh();
        } catch (AuthorizationException | ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML private void handleToggleMachine() {
        CncMachine selected = machinesList.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Odaberite CNC stroj."); return; }
        try {
            managementService.setMachineActive(selected.getCncMachineId(), !selected.isActive());
            refresh();
        } catch (AuthorizationException | ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML private void handleToggleTool() {
        Tool selected = toolsList.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Odaberite alat."); return; }
        try {
            managementService.setToolActive(selected.getToolId(), !selected.isActive());
            refresh();
        } catch (AuthorizationException | ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML private void handleBack() { navigation.showMain(); }

    @FXML private void handleLogout() {
        sessionContext.logout();
        navigation.showLogin();
    }

    private <T> void showDialog(String resource, String title, Class<T> type, Consumer<T> beforeShow) {
        if (!authorizationService.isAllowed(AuthorizationService.Permission.MANAGE_REFERENCE_DATA)) {
            statusLabel.setText("Nemate pravo uređivati katalog.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            loader.setControllerFactory(controllerFactory);
            Parent root = loader.load();
            beforeShow.accept(type.cast(loader.getController()));
            Stage dialog = new Stage();
            dialog.setTitle(title);
            dialog.initOwner(statusLabel.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
            refresh();
            statusLabel.setText("Katalog je osvježen.");
        } catch (IOException exception) {
            statusLabel.setText("Otvaranje obrasca nije uspjelo: " + exception.getMessage());
        }
    }

    private void refresh() {
        try {
            Long selectedMachineId = toolMachineComboBox.getValue() == null
                    ? null : toolMachineComboBox.getValue().getCncMachineId();
            materialTypesList.getItems().setAll(managementService.loadMaterialTypes());
            var machines = managementService.loadMachines();
            machinesList.getItems().setAll(machines);
            toolMachineComboBox.getItems().setAll(machines);
            CncMachine selectedMachine = machines.stream()
                    .filter(machine -> selectedMachineId != null
                            && selectedMachineId.equals(machine.getCncMachineId()))
                    .findFirst()
                    .orElse(machines.isEmpty() ? null : machines.getFirst());
            toolMachineComboBox.setValue(selectedMachine);
            loadToolsForSelectedMachine();
        } catch (AuthorizationException | ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
        refreshSavedPrograms();
    }

    private void loadToolsForSelectedMachine() {
        CncMachine machine = toolMachineComboBox.getValue();
        if (machine == null) {
            toolsList.getItems().clear();
            return;
        }
        try {
            toolsList.getItems().setAll(managementService.loadTools(machine));
        } catch (AuthorizationException | ReferenceDataAccessException exception) {
            toolsList.getItems().clear();
            statusLabel.setText(exception.getMessage());
        }
    }

    private StringConverter<CncMachine> machineConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(CncMachine machine) {
                return machine == null ? "" : machine.getName() + " — " + machine.getController()
                        + (machine.isActive() ? " — aktivno" : " — deaktivirano");
            }

            @Override
            public CncMachine fromString(String value) {
                throw new UnsupportedOperationException("Odabir stroja nije tekstualno uređiv.");
            }
        };
    }

    private void refreshSavedPrograms() {
        try {
            authorizationService.require(AuthorizationService.Permission.MANAGE_REFERENCE_DATA);
            savedProgramsList.getItems().setAll(savedJobService.loadAll());
            savedProgramPreview.clear();
        } catch (AuthorizationException | SavedJobAccessException exception) {
            savedProgramsList.getItems().clear();
            savedProgramPreview.clear();
            statusLabel.setText(exception.getMessage());
        }
    }
}
