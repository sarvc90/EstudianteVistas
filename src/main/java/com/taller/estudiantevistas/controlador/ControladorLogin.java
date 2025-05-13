package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class ControladorLogin {

    @FXML
    private TextField nombreField;

    @FXML
    private PasswordField contrasenaField;

    @FXML
    private Label registerLink;

    @FXML
    public void iniciarSesion() {
        String nombre = nombreField.getText();
        String contrasena = contrasenaField.getText();

        // Aquí puedes poner la lógica de autenticación e interacción con el servidor
        System.out.println("Intentando iniciar sesión con:");
        System.out.println("Nombre: " + nombre);
        System.out.println("Contraseña: " + contrasena);

        // Todo: reemplazar por llamada a backend, mostrar mensajes, validaciones, etc.
    }

    @FXML
    public void registrarse(MouseEvent event) {
        try {
            // Cerrar la ventana actual
            Stage stage = (Stage) nombreField.getScene().getWindow();

            // Cargar la vista de registro
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/registro.fxml"));
            Parent root = loader.load();

            Stage registroStage = new Stage();
            registroStage.setScene(new Scene(root));
            registroStage.setTitle("Registro - Red Social Educativa");
            registroStage.show();

            stage.close(); // Cierra la ventana de login
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
