package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.service.AuthorizationService;
import hr.lukabosnjak.service.AuthorizationException;
import hr.lukabosnjak.service.ReferenceDataAccessException;
import hr.lukabosnjak.service.ReferenceDataManagementService;
import hr.lukabosnjak.service.SessionContext;
import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.domain.entities.Tool;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.function.Consumer;

public final class CatalogController {
    @FXML private Button addMaterialTypeButton;
    @FXML private Button addMachineButton;
    @FXML private Button addToolButton;
    @FXML private Label currentUserLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<MaterialType> materialTypesList;
    @FXML private ListView<CncMachine> machinesList;
    @FXML private ListView<Tool> toolsList;

    private final ReferenceDataManagementService managementService;
    private final AuthorizationService authorizationService;
    private final SessionContext sessionContext;
    private final ApplicationNavigation navigation;
    private final Callback<Class<?>, Object> controllerFactory;

    public CatalogController(ReferenceDataManagementService managementService,
                             AuthorizationService authorizationService, SessionContext sessionContext,
                             ApplicationNavigation navigation, Callback<Class<?>, Object> controllerFactory) {
        this.managementService = managementService;
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
        statusLabel.setText("Odaberite katalog koji želite urediti.");
        materialTypesList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(MaterialType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        machinesList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(CncMachine item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " — " + item.getController());
            }
        });
        toolsList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Tool item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "T" + item.getToolNumber()
                        + " — Ø" + item.getDiameter() + " mm — " + item.getName());
            }
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
        CncMachine machine = machinesList.getSelectionModel().getSelectedItem();
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
            statusLabel.setText("Katalog je osvježen.");
        } catch (IOException exception) {
            statusLabel.setText("Otvaranje obrasca nije uspjelo: " + exception.getMessage());
        }
    }

    private void refresh() {
        try {
            materialTypesList.getItems().setAll(managementService.loadMaterialTypes());
            machinesList.getItems().setAll(managementService.loadMachines());
            CncMachine machine = machinesList.getSelectionModel().getSelectedItem();
            if (machine != null) {
                toolsList.getItems().setAll(managementService.loadTools(machine));
            } else {
                toolsList.getItems().clear();
            }
        } catch (AuthorizationException | ReferenceDataAccessException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }
}
