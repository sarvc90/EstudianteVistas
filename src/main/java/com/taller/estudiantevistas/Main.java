package com.taller.estudiantevistas;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Cargar vista FXML usando getResource con ruta absoluta
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/taller/estudiantevistas/fxml/login.fxml"));
        Parent root = loader.load();

        // Configurar escena
        Scene scene = new Scene(root);

        // Configurar ventana
        primaryStage.setTitle("Login - Red Social Educativa");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}