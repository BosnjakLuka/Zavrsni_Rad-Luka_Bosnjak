package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.service.AuthService;
import hr.lukabosnjak.service.AuthenticationException;
import hr.lukabosnjak.service.RegistrationException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Arrays;
import java.util.Objects;

public final class RegistrationController {
    @FXML private TextField usernameInput;
    @FXML private TextField firstNameInput;
    @FXML private TextField lastNameInput;
    @FXML private PasswordField passwordInput;
    @FXML private PasswordField confirmPasswordInput;
    @FXML private Label statusLabel;

    private final AuthService authService;
    private final ApplicationNavigation navigation;

    public RegistrationController(AuthService authService, ApplicationNavigation navigation) {
        this.authService = Objects.requireNonNull(authService);
        this.navigation = Objects.requireNonNull(navigation);
    }

    @FXML
    private void handleRegister() {
        char[] password = passwordInput.getText().toCharArray();
        char[] confirmation = confirmPasswordInput.getText().toCharArray();
        try {
            if (!Arrays.equals(password, confirmation)) {
                statusLabel.setText("Lozinka i potvrda lozinke nisu jednake.");
                return;
            }
            authService.register(
                    usernameInput.getText(), password, firstNameInput.getText(), lastNameInput.getText());
            authService.login(usernameInput.getText(), password);
            clearPasswordFields();
            navigation.showMain();
        } catch (RegistrationException | AuthenticationException exception) {
            clearPasswordFields();
            statusLabel.setText(exception.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }

    @FXML
    private void handleBackToLogin() {
        clearPasswordFields();
        navigation.showLogin();
    }

    private void clearPasswordFields() {
        passwordInput.clear();
        confirmPasswordInput.clear();
    }
}
