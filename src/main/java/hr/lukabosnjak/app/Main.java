package hr.lukabosnjak.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import hr.lukabosnjak.service.AuthorizationService;
import hr.lukabosnjak.ui.controller.ApplicationNavigation;

import java.io.IOException;

public class Main extends Application implements ApplicationNavigation {
    private final ApplicationCompositionRoot compositionRoot = ApplicationCompositionRoot.production();
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        compositionRoot.initializeDatabase();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("CNC Optimizer");
        showLogin();
        primaryStage.show();
    }

    @Override
    public void showLogin() {
        compositionRoot.authService().logout();
        showView("/hr/lukabosnjak/ui/view/login.fxml", 440, 420, 400, 360);
    }

    @Override
    public void showRegistration() {
        showView("/hr/lukabosnjak/ui/view/registration.fxml", 480, 580, 440, 520);
    }

    @Override
    public void showMain() {
        if (!compositionRoot.sessionContext().isAuthenticated()) {
            showLogin();
            return;
        }

        showView("/hr/lukabosnjak/ui/view/main-form.fxml", 1100, 760, 900, 650);
    }

    @Override
    public void showUserManagement() {
        if (!compositionRoot.authorizationService().isAllowed(
                AuthorizationService.Permission.MANAGE_USERS)) {
            showMain();
            return;
        }
        showView("/hr/lukabosnjak/ui/view/user-management.fxml", 850, 560, 700, 450);
    }

    private void showView(String resourcePath, double width, double height, double minWidth, double minHeight) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(resourcePath));
            loader.setControllerFactory(type -> compositionRoot.createController(type, this));
            primaryStage.setScene(new Scene(loader.load(), width, height));
            primaryStage.setMinWidth(minWidth);
            primaryStage.setMinHeight(minHeight);
            primaryStage.centerOnScreen();
        } catch (IOException exception) {
            throw new IllegalStateException("UI prikaz nije moguće učitati: " + resourcePath, exception);
        }
    }
}
