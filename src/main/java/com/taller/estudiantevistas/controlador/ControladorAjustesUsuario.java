package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import com.google.gson.JsonObject;

import java.util.Optional;

public class ControladorAjustesUsuario {
    @FXML private TextField nombreField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private ClienteServicio cliente;
    private String usuarioId;
    private Stage stage;

    public void inicializar(JsonObject usuarioData, ClienteServicio cliente, Stage stage) {
        this.cliente = cliente;
        this.usuarioId = usuarioData.get("id").getAsString();
        this.stage = stage;

        // Cargar datos del usuario en los campos
        nombreField.setText(usuarioData.get("nombre").getAsString());
        emailField.setText(usuarioData.get("email").getAsString());
        usuarioField.setText(usuarioData.get("username").getAsString());
        passwordField.setText(""); // La contraseña no debería mostrarse
    }

    @FXML
    private void actualizarDatos() {
        JsonObject datosActualizados = new JsonObject();
        datosActualizados.addProperty("id", usuarioId);
        datosActualizados.addProperty("nombre", nombreField.getText());
        datosActualizados.addProperty("email", emailField.getText());
        datosActualizados.addProperty("password", passwordField.getText());

        // Enviar al servidor
        JsonObject respuesta = cliente.actualizarUsuario(datosActualizados);

        if (respuesta.get("exito").getAsBoolean()) {
            mostrarAlerta("Éxito", "Datos actualizados correctamente", Alert.AlertType.INFORMATION);
        } else {
            mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void solicitarEliminarCuenta() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro de eliminar su cuenta?");
        confirmacion.setContentText("Esta acción no se puede deshacer. Todos sus datos se perderán permanentemente.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            // Usuario confirmó, proceder a eliminar
            JsonObject respuesta = cliente.eliminarUsuario(usuarioId);

            if (respuesta.get("exito").getAsBoolean()) {
                mostrarAlerta("Éxito", "Cuenta eliminada correctamente", Alert.AlertType.INFORMATION);
                stage.close(); // Cerrar ventana de ajustes
                // Aquí deberías también cerrar la sesión en la aplicación principal
            } else {
                mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR);
            }
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}