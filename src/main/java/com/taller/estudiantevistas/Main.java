package com.taller.estudiantevistas;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Cargar la vista FXML directamente
        Parent root = FXMLLoader.load(getClass().getResource("/com/taller/estudiantevistas/fxml/login.fxml"));

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