package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.HashSet;
import java.util.Set;

/**
 * Controlador para la vista de contacto, que permite al usuario ver grupos de estudio
 * y enviar mensajes a los grupos seleccionados.
 */

public class ControladorContacto {
    @FXML
    private ListView<String> listaGrupos;
    @FXML
    private TextArea areaMensaje;
    @FXML
    private Button btnEnviar;

    private JsonArray grupos;
    private JsonObject usuarioData;
    private ClienteServicio cliente;
    private Stage primaryStage;

    /**
     * Inicializa el controlador con los datos del usuario y los grupos de estudio.
     * Configura la lista de grupos y el evento de doble clic para abrir el detalle del grupo.
     *
     * @param grupos       Lista de grupos de estudio en formato JSON.
     * @param usuarioData  Datos del usuario en formato JSON.
     * @param cliente      ClienteServicio para la comunicación con el servidor.
     */

    public void inicializar(JsonArray grupos, JsonObject usuarioData, ClienteServicio cliente) {
        this.grupos = grupos;
        this.usuarioData = usuarioData;
        this.cliente = cliente;
        cargarGrupos();
        listaGrupos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                abrirDetalleGrupo();
            }
        });
    }

    /**
     * Carga los grupos de estudio en la lista, asegurando que no haya duplicados.
     * Cada grupo se muestra con su nombre y descripción.
     */

    private void cargarGrupos() {
        listaGrupos.getItems().clear();
        Set<String> gruposUnicos = new HashSet<>();

        for (JsonElement elemento : grupos) {
            JsonObject grupo = elemento.getAsJsonObject();
            String grupoId = grupo.get("id").getAsString();

            if (!gruposUnicos.contains(grupoId)) {
                String nombre = grupo.get("nombre").getAsString();
                String descripcion = grupo.get("descripcion").getAsString();
                listaGrupos.getItems().add(nombre + ": " + descripcion);
                gruposUnicos.add(grupoId);
            }
        }

        if (!listaGrupos.getItems().isEmpty()) {
            listaGrupos.getSelectionModel().selectFirst();
        }
    }

    /**
     * Envía el mensaje al grupo seleccionado.
     * Verifica que se haya seleccionado un grupo y que el mensaje no esté vacío.
     */

    private void abrirDetalleGrupo() {
        try {
            int indice = listaGrupos.getSelectionModel().getSelectedIndex();
            if (indice >= 0) {
                JsonObject grupo = grupos.get(indice).getAsJsonObject();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/grupo-estudio.fxml"));
                Parent root = loader.load();

                ControladorGrupoEstudio controlador = loader.getController();

                controlador.inicializar(
                        grupo,
                        cliente.getSalida(),
                        cliente.getEntrada(),
                        obtenerStageActual()
                );

                Stage stage = new Stage();
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initOwner(obtenerStageActual());
                stage.setScene(new Scene(root));
                stage.setTitle("Detalle del Grupo");
                stage.show();
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo abrir el detalle del grupo: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Envía el mensaje al grupo seleccionado.
     * Verifica que se haya seleccionado un grupo y que el mensaje no esté vacío.
     */

    private Stage obtenerStageActual() {
        if (listaGrupos != null && listaGrupos.getScene() != null && listaGrupos.getScene().getWindow() != null) {
            return (Stage) listaGrupos.getScene().getWindow();
        }
        return null;
    }

    /**
     * Envía el mensaje al grupo seleccionado.
     * Verifica que se haya seleccionado un grupo y que el mensaje no esté vacío.
     */
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
    }
}