package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import com.google.gson.JsonObject;

public class ControladorSolicitudAyuda {
    @FXML private TextField tituloField;
    @FXML private TextField temaField;
    @FXML private TextField contenidoField;

    private ClienteServicio cliente;
    private String usuarioId;

    public void inicializar(String usuarioId, ClienteServicio cliente) {
        this.usuarioId = usuarioId;
        this.cliente = cliente;
    }

    @FXML
    private void mostrarOpcionesUrgencia() {
        ContextMenu menu = new ContextMenu();

        MenuItem alta = new MenuItem("Alta");
        alta.setOnAction(e -> mostrarAlerta("Urgencia seleccionada", "Urgencia: Alta", AlertType.INFORMATION));

        MenuItem media = new MenuItem("Media");
        media.setOnAction(e -> mostrarAlerta("Urgencia seleccionada", "Urgencia: Media", AlertType.INFORMATION));

        MenuItem baja = new MenuItem("Baja");
        baja.setOnAction(e -> mostrarAlerta("Urgencia seleccionada", "Urgencia: Baja", AlertType.INFORMATION));

        menu.getItems().addAll(alta, media, baja);
        menu.show(tituloField.getScene().getWindow());
    }

    @FXML
    private void publicarSolicitud() {
        if (validarCampos()) {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("titulo", tituloField.getText());
            solicitud.addProperty("tema", temaField.getText());
            solicitud.addProperty("descripcion", contenidoField.getText());
            solicitud.addProperty("usuarioId", usuarioId);

            // Aquí implementarías el envío al servidor
            mostrarAlerta("Éxito", "Solicitud creada correctamente", AlertType.INFORMATION);
            limpiarCampos();
        }
    }

    private boolean validarCampos() {
        if (tituloField.getText().isEmpty() || temaField.getText().isEmpty() || contenidoField.getText().isEmpty()) {
            mostrarAlerta("Error", "Todos los campos son obligatorios", AlertType.ERROR);
            return false;
        }
        return true;
    }

    private void limpiarCampos() {
        tituloField.clear();
        temaField.clear();
        contenidoField.clear();
    }

    private void mostrarAlerta(String titulo, String mensaje, AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}