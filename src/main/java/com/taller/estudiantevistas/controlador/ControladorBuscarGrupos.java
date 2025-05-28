package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class ControladorBuscarGrupos {

    private ClienteServicio cliente;
    private String usuarioId; // Almacena el ID del usuario


    @FXML private ListView<String> listViewGrupos;
    @FXML private Button btnUnirse;
    public void inicializar(ClienteServicio cliente, String usuarioId) {
        this.cliente = cliente;
        this.usuarioId = usuarioId; // Asignar el ID del usuario
        cargarGrupos();
    }

    private void cargarGrupos() {
        // Lógica para cargar grupos desde el servidor
        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "OBTENER_GRUPOS_ESTUDIO"); // Asegúrate de que esto esté presente
        JsonObject datos = new JsonObject();
        datos.addProperty("userId", usuarioId); // Asegúrate de que el ID del usuario esté presente
        solicitud.add("datos", datos); // Agregar los datos a la solicitud
        cliente.getSalida().println(solicitud.toString());
        // Esperar respuesta del servidor
        String respuesta;
        try {
            respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
            if (jsonRespuesta.get("exito").getAsBoolean()) {
                JsonArray grupos = jsonRespuesta.getAsJsonArray("grupos");
                for (int i = 0; i < grupos.size(); i++) {
                    String nombreGrupo = grupos.get(i).getAsJsonObject().get("nombre").getAsString();
                    // Agregar solo si no está ya en el ListView
                    if (!listViewGrupos.getItems().contains(nombreGrupo)) {
                        listViewGrupos.getItems().add(nombreGrupo);
                    }
                }
            } else {
                mostrarAlerta("Error", jsonRespuesta.get("mensaje").getAsString());
            }
        } catch (IOException e) {
            mostrarAlerta("Error", "Error de comunicación con el servidor: " + e.getMessage());
        }
    }

    @FXML
    private void unirseGrupo() {
        String grupoSeleccionado = listViewGrupos.getSelectionModel().getSelectedItem();
        if (grupoSeleccionado == null) {
            mostrarAlerta("Error", "Debes seleccionar un grupo para unirte.");
            return;
        }

        // Lógica para unirse al grupo
        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "UNIRSE_GRUPO");
        JsonObject datos = new JsonObject();
        datos.addProperty("grupoId", grupoSeleccionado); // Asegúrate de que el ID del grupo esté disponible
        datos.addProperty("usuarioId", usuarioId); // Usar el ID del usuario almacenado
        solicitud.add("datos", datos);
        cliente.getSalida().println(solicitud.toString());
        // Esperar respuesta del servidor
        String respuesta;
        try {
            respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
            if (jsonRespuesta.get("exito").getAsBoolean()) {
                mostrarAlerta("Éxito", "Te has unido al grupo " + grupoSeleccionado + " exitosamente.");
                // Cerrar la ventana
                Stage stage = (Stage) btnUnirse.getScene().getWindow();
                stage.close();
            } else {
                mostrarAlerta("Error", jsonRespuesta.get("mensaje").getAsString());
            }
        } catch (IOException e) {
            mostrarAlerta("Error", "Error de comunicación con el servidor: " + e.getMessage());
        }
    }
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}