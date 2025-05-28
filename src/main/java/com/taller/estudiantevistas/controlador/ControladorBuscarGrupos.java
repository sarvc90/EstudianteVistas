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

/**
 * Controlador para la vista de búsqueda de grupos de estudio.
 * Permite a los usuarios buscar y unirse a grupos existentes.
 */

public class ControladorBuscarGrupos {

    private ClienteServicio cliente;
    private String usuarioId;


    @FXML private ListView<String> listViewGrupos;
    @FXML private Button btnUnirse;

    /**
     * Inicializa el controlador con el cliente y el ID del usuario.
     * @param cliente ClienteServicio para la comunicación con el servidor.
     * @param usuarioId ID del usuario que busca unirse a grupos.
     */

    public void inicializar(ClienteServicio cliente, String usuarioId) {
        this.cliente = cliente;
        this.usuarioId = usuarioId;
        cargarGrupos();
    }

    /**
     * Carga los grupos de estudio disponibles desde el servidor y los muestra en el ListView.
     * Se asegura de que no se agreguen grupos duplicados.
     */

    private void cargarGrupos() {
        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "OBTENER_GRUPOS_ESTUDIO");
        JsonObject datos = new JsonObject();
        datos.addProperty("userId", usuarioId);
        solicitud.add("datos", datos);
        cliente.getSalida().println(solicitud.toString());
        String respuesta;
        try {
            respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
            if (jsonRespuesta.get("exito").getAsBoolean()) {
                JsonArray grupos = jsonRespuesta.getAsJsonArray("grupos");
                for (int i = 0; i < grupos.size(); i++) {
                    String nombreGrupo = grupos.get(i).getAsJsonObject().get("nombre").getAsString();
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

    /**
     * Maneja el evento de unirse a un grupo seleccionado.
     * Verifica que se haya seleccionado un grupo y envía la solicitud al servidor.
     */

    @FXML
    private void unirseGrupo() {
        String grupoSeleccionado = listViewGrupos.getSelectionModel().getSelectedItem();
        if (grupoSeleccionado == null) {
            mostrarAlerta("Error", "Debes seleccionar un grupo para unirte.");
            return;
        }


        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "UNIRSE_GRUPO");
        JsonObject datos = new JsonObject();
        datos.addProperty("grupoId", grupoSeleccionado);
        datos.addProperty("usuarioId", usuarioId);
        solicitud.add("datos", datos);
        cliente.getSalida().println(solicitud.toString());
        String respuesta;
        try {
            respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
            if (jsonRespuesta.get("exito").getAsBoolean()) {
                mostrarAlerta("Éxito", "Te has unido al grupo " + grupoSeleccionado + " exitosamente.");
                Stage stage = (Stage) btnUnirse.getScene().getWindow();
                stage.close();
            } else {
                mostrarAlerta("Error", jsonRespuesta.get("mensaje").getAsString());
            }
        } catch (IOException e) {
            mostrarAlerta("Error", "Error de comunicación con el servidor: " + e.getMessage());
        }
    }

    /**
     * Muestra una alerta con el título y mensaje proporcionados.
     * @param titulo Título de la alerta.
     * @param mensaje Mensaje de la alerta.
     */

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}