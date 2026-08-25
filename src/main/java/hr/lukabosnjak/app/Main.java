package hr.lukabosnjak.app;

import hr.lukabosnjak.config.DatabaseInitializer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        DatabaseInitializer.initialize();
    }

    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(new Group(), 640, 480));
        stage.show();
    }
}
