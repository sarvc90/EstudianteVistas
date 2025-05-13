package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class ControladorRegistro {

    @FXML private TextField nombresField;
    @FXML private TextField apellidosField;
    @FXML private TextField cedulaField;
    @FXML private PasswordField contrasenaField;
    @FXML private TextField correoField;
    @FXML private TextField interesesField;
    @FXML
    private Button backButton;

    @FXML
    private void registrarUsuario() {
        String nombres = nombresField.getText();
        String apellidos = apellidosField.getText();
        String cedula = cedulaField.getText();
        String contrasena = contrasenaField.getText();
        String correo = correoField.getText();
        String intereses = interesesField.getText();

        // Validación básica
        if (nombres.isEmpty() || apellidos.isEmpty() || cedula.isEmpty() ||
                contrasena.isEmpty() || correo.isEmpty()) {
            mostrarAlerta("Error", "Todos los campos son obligatorios excepto intereses");
            return;
        }

        // Aquí iría la lógica para registrar al usuario
        System.out.println("Registrando usuario:");
        System.out.println("Nombres: " + nombres);
        System.out.println("Apellidos: " + apellidos);
        System.out.println("Cédula: " + cedula);
        System.out.println("Correo: " + correo);
        System.out.println("Intereses: " + intereses);

        mostrarAlerta("Éxito", "Usuario registrado correctamente");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void volverALogin(ActionEvent event) {  // Añade el parámetro ActionEvent
        try {
            // Obtener la ventana actual desde el evento
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();

            // Cargar la vista de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/login.fxml"));
            Parent root = loader.load();

            // Aplicar estilos CSS
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/taller/estudiantevistas/css/estilos.css").toExternalForm());

            // Configurar la nueva ventana
            Stage loginStage = new Stage();
            loginStage.setScene(scene);
            loginStage.setTitle("Login - Red Social Educativa");
            loginStage.show();

            stage.close();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo cargar la ventana de login");
            e.printStackTrace();
        }
    }
}