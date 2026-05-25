package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 520, 700);
        // Cargar ambas hojas de estilo
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/tabs.css").toExternalForm());

        primaryStage.setTitle("Database Manager Tool — MariaDB");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
