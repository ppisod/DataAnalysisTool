package org.jackl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jackl.Data.Database;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("jackl-data-analysis");

        Parent root = FXMLLoader.load(getClass().getResource("Layouts/MainScreen.fxml"));

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Override
    public void stop() {
        Database.close();
    }
}
