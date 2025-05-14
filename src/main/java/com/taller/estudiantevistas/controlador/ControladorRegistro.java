package com.taller.estudiantevistas.controlador;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.taller.estudiantevistas.dto.Estudiante;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

public class ControladorRegistro {

    @FXML private TextField nombresField;
    @FXML private TextField apellidosField;
    @FXML private TextField cedulaField;
    @FXML private PasswordField contrasenaField;
    @FXML private TextField correoField;
    @FXML private TextField interesesField;
    @FXML private Button backButton;

    private ClienteServicio clienteServicio;
    private Gson gson;

    public ControladorRegistro() {
        try {
            this.clienteServicio = new ClienteServicio("localhost", 12345);
            this.gson = new Gson();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo conectar al servidor.");
            e.printStackTrace();
        }
    }

    @FXML
    private void registrarUsuario() {
        String nombres = nombresField.getText().trim();
        String apellidos = apellidosField.getText().trim();
        String cedula = cedulaField.getText().trim();
        String contrasena = contrasenaField.getText().trim();
        String correo = correoField.getText().trim();
        String intereses = interesesField.getText().trim();

        // Validación de campos
        if (nombres.isEmpty() || apellidos.isEmpty() || cedula.isEmpty() ||
                contrasena.isEmpty() || correo.isEmpty()) {
            mostrarAlerta("Error", "Todos los campos son obligatorios excepto intereses.");
            return;
        }

        if (!correo.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            mostrarAlerta("Error", "Por favor ingrese un correo electrónico válido.");
            return;
        }

        try {
            Estudiante nuevoEstudiante = new Estudiante(nombres, apellidos, cedula, correo, contrasena, intereses);

            // 📌 Corrección: Enviar datos dentro del objeto "datos"
            JsonObject mensaje = new JsonObject();
            mensaje.addProperty("tipo", "REGISTRO");
            JsonObject datos = gson.toJsonTree(nuevoEstudiante).getAsJsonObject();
            mensaje.add("datos", datos);

            // 📤 Verificación antes de enviar la solicitud
            System.out.println("📤 Enviando solicitud de registro...");
            System.out.println("JSON enviado al servidor: " + mensaje.toString());

            boolean registroExitoso = clienteServicio.registrarEstudiante(nuevoEstudiante);

            if (registroExitoso) {
                mostrarAlerta("✅ Éxito", "Usuario registrado correctamente.");
                limpiarCampos();
            } else {
                mostrarAlerta("❌ Error", "No se pudo registrar el usuario. Cédula o correo ya existen.");
            }
        } catch (Exception e) {
            mostrarAlerta("❌ Error", "Ocurrió un problema al registrar el usuario.");
            e.printStackTrace();
        }
    }

    private void limpiarCampos() {
        nombresField.clear();
        apellidosField.clear();
        cedulaField.clear();
        contrasenaField.clear();
        correoField.clear();
        interesesField.clear();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void volverALogin(ActionEvent event) {
        try {
            if (clienteServicio != null) {
                clienteServicio.cerrarConexion();
            }

            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/taller/estudiantevistas/css/estilos.css").toExternalForm());

            Stage loginStage = new Stage();
            loginStage.setScene(scene);
            loginStage.setTitle("Login - Red Social Educativa");
            loginStage.show();

            stage.close();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo cargar la ventana de login.");
            e.printStackTrace();
        }
    }
}