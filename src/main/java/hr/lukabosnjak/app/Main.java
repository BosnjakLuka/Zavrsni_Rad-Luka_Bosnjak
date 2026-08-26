package hr.lukabosnjak.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    private final ApplicationCompositionRoot compositionRoot = ApplicationCompositionRoot.production();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        compositionRoot.initializeDatabase();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/hr/lukabosnjak/ui/view/main-form.fxml"));
        loader.setControllerFactory(compositionRoot::createController);
        Scene scene = new Scene(loader.load(), 1100, 760);
        stage.setTitle("CNC Optimizer");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }
}
