package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonObject;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ControladorSolicitudAyuda {
    @FXML private TextField tituloField;
    @FXML private TextField temaField;
    @FXML private TextField contenidoField;
    @FXML private Button urgenciaButton;

    private JsonObject usuarioData;
    private ClienteServicio cliente;
    private String urgencia = "Media";

    public void inicializar(JsonObject usuarioData, ClienteServicio cliente) {
        this.usuarioData = usuarioData;
        this.cliente = cliente;
    }

    @FXML
    private void mostrarOpcionesUrgencia() {
        ContextMenu menu = new ContextMenu();

        MenuItem alta = new MenuItem("Alta");
        alta.setOnAction(e -> {
            urgencia = "Alta";
            urgenciaButton.setText("Urgencia: Alta");
        });

        MenuItem media = new MenuItem("Media");
        media.setOnAction(e -> {
            urgencia = "Media";
            urgenciaButton.setText("Urgencia: Media");
        });

        MenuItem baja = new MenuItem("Baja");
        baja.setOnAction(e -> {
            urgencia = "Baja";
            urgenciaButton.setText("Urgencia: Baja");
        });

        menu.getItems().addAll(alta, media, baja);

        menu.show(urgenciaButton, urgenciaButton.localToScreen(0, 0).getX(),
                urgenciaButton.localToScreen(0, 0).getY() + urgenciaButton.getHeight());
    }

    @FXML
    private void publicarSolicitud() {
        if (validarCampos()) {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "CREAR_SOLICITUD");

            JsonObject datos = new JsonObject();
            datos.addProperty("usuarioId", usuarioData.get("id").getAsString());
            datos.addProperty("titulo", tituloField.getText());
            datos.addProperty("tema", temaField.getText());
            datos.addProperty("descripcion", contenidoField.getText());
            datos.addProperty("urgencia", urgencia);

            solicitud.add("datos", datos);


            cliente.getSalida().println(solicitud.toString());

            mostrarAlerta("Éxito", "Solicitud creada correctamente", AlertType.INFORMATION);
            limpiarCampos();
            cerrarVentana();
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

    private void cerrarVentana() {
        Stage stage = (Stage) tituloField.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje, AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}