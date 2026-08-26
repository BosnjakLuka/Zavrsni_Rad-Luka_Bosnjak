package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.service.AuthService;
import hr.lukabosnjak.service.AuthenticationException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Arrays;
import java.util.Objects;

public final class LoginController {
    @FXML private TextField usernameInput;
    @FXML private PasswordField passwordInput;
    @FXML private Label statusLabel;

    private final AuthService authService;
    private final ApplicationNavigation navigation;

    public LoginController(AuthService authService, ApplicationNavigation navigation) {
        this.authService = Objects.requireNonNull(authService);
        this.navigation = Objects.requireNonNull(navigation);
    }

    @FXML
    private void handleLogin() {
        char[] password = passwordInput.getText().toCharArray();
        try {
            authService.login(usernameInput.getText(), password);
            passwordInput.clear();
            navigation.showMain();
        } catch (AuthenticationException exception) {
            passwordInput.clear();
            statusLabel.setText(exception.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @FXML
    private void handleRegistration() {
        navigation.showRegistration();
    }
}
