package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ControladorContenidosPerfil {
    @FXML private ScrollPane scrollPane;
    @FXML private VBox contenedorContenidos;

    private String userId;
    private BiConsumer<String, Consumer<JsonArray>> cargadorContenidos;

    public void inicializar(String userId, Object cliente, BiConsumer<String, Consumer<JsonArray>> cargador) {
        this.userId = userId;
        this.cargadorContenidos = cargador;
        cargarContenidos();
    }

    private void cargarContenidos() {
        contenedorContenidos.getChildren().clear();
        contenedorContenidos.getChildren().add(new Label("Cargando contenidos..."));

        cargadorContenidos.accept(userId, contenidos -> {
            contenedorContenidos.getChildren().clear();

            if (contenidos.size() == 0) {
                Label mensaje = new Label("No hay contenidos publicados");
                mensaje.getStyleClass().add("mensaje-vacio");
                contenedorContenidos.getChildren().add(mensaje);
            } else {
                contenidos.forEach(contenido -> {
                    JsonObject cont = contenido.getAsJsonObject();
                    VBox item = crearItemContenido(cont);
                    contenedorContenidos.getChildren().add(item);
                });
            }
        });
    }

    private VBox crearItemContenido(JsonObject contenido) {
        VBox item = new VBox(8);
        item.getStyleClass().add("contenido-item");

        Label titulo = new Label(contenido.get("titulo").getAsString());
        titulo.getStyleClass().add("titulo");

        Text descripcion = new Text(contenido.get("descripcion").getAsString());
        descripcion.getStyleClass().add("descripcion");
        descripcion.setWrappingWidth(scrollPane.getWidth() - 30);

        HBox metaInfo = new HBox(10);
        metaInfo.getStyleClass().add("meta-info");
        metaInfo.getChildren().addAll(
                new Label("Tipo: " + contenido.get("tipo").getAsString()),
                new Label("Tema: " + contenido.get("tema").getAsString())
        );

        item.getChildren().addAll(titulo, descripcion, metaInfo);
        return item;
    }
}