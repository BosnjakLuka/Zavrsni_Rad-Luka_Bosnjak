package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.entities.Role;
import hr.lukabosnjak.domain.entities.User;
import hr.lukabosnjak.service.AuthorizationException;
import hr.lukabosnjak.service.UserManagementException;
import hr.lukabosnjak.service.UserManagementService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public final class UserManagementController {
    @FXML private ListView<User> usersList;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private Button toggleActiveButton;
    @FXML private Label statusLabel;

    private final UserManagementService userManagementService;
    private final ApplicationNavigation navigation;

    public UserManagementController(UserManagementService userManagementService, ApplicationNavigation navigation) {
        this.userManagementService = userManagementService;
        this.navigation = navigation;
    }

    @FXML
    private void initialize() {
        usersList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null
                        : user.getUsername() + " — " + user.getRole().getName()
                        + (user.isActive() ? " (aktivan)" : " (deaktiviran)"));
            }
        });
        roleComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Role role) { return role == null ? "" : role.getName(); }
            @Override public Role fromString(String value) { throw new UnsupportedOperationException(); }
        });
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> {
            roleComboBox.setValue(user == null ? null : user.getRole());
            toggleActiveButton.setText(user != null && user.isActive() ? "Deaktiviraj" : "Aktiviraj");
        });
        refresh();
    }

    @FXML
    private void handleToggleActive() {
        User selected = selected();
        if (selected == null) return;
        try {
            userManagementService.setActive(selected, !selected.isActive());
            refresh();
            statusLabel.setText("Status korisnika je promijenjen.");
        } catch (AuthorizationException | UserManagementException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleChangeRole() {
        User selected = selected();
        Role role = roleComboBox.getValue();
        if (selected == null || role == null) return;
        try {
            userManagementService.changeRole(selected, role);
            refresh();
            statusLabel.setText("Rola korisnika je promijenjena.");
        } catch (AuthorizationException | UserManagementException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        navigation.showMain();
    }

    private void refresh() {
        try {
            usersList.getItems().setAll(userManagementService.loadUsers());
            roleComboBox.getItems().setAll(userManagementService.loadRoles());
        } catch (AuthorizationException | UserManagementException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private User selected() {
        return usersList.getSelectionModel().getSelectedItem();
    }
}
