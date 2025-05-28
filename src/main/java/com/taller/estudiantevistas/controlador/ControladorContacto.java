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

    // CORRECCIÓN: Cambiar la firma del método para que coincida con la llamada en ControladorPrincipal
    public void inicializar(JsonArray grupos, JsonObject usuarioData, ClienteServicio cliente) {
        this.grupos = grupos;
        this.usuarioData = usuarioData;
        this.cliente = cliente;
        cargarGrupos();

        // Configurar doble clic para abrir detalle
        listaGrupos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                abrirDetalleGrupo();
            }
        });
    }

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

    // Método auxiliar para obtener el Stage actual
    private Stage obtenerStageActual() {
        if (listaGrupos != null && listaGrupos.getScene() != null && listaGrupos.getScene().getWindow() != null) {
            return (Stage) listaGrupos.getScene().getWindow();
        }
        return null;
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
    }
}