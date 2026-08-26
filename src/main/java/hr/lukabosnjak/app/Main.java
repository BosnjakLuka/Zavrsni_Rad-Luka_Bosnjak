package hr.lukabosnjak.app;

import hr.lukabosnjak.config.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        DatabaseInitializer.initialize();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/hr/lukabosnjak/ui/view/main-form.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 760);
        stage.setTitle("CNC Optimizer");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }
}
