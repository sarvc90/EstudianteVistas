package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controlador para la vista de búsqueda de contenidos.
 * Muestra los resultados de búsqueda y permite ver detalles de cada contenido.
 */

public class ControladorBuscar {
    @FXML private VBox resultsContainer;
    @FXML private Label titleLabel;

    private JsonArray resultadosBusqueda;
    private String tipoBusqueda;
    private String terminoBusqueda;
    private ClienteServicio cliente;
    private JsonObject usuarioData;

    /**
     * Inicializa el controlador con los resultados de búsqueda y otros parámetros.
     *
     * @param resultados Resultados de búsqueda en formato JSON.
     * @param tipoBusqueda Tipo de búsqueda (ej. "Contenido", "Usuario").
     * @param terminoBusqueda Término de búsqueda utilizado.
     * @param cliente Cliente del servicio para operaciones adicionales.
     * @param usuarioData Datos del usuario actual.
     */

    public void inicializar(JsonArray resultados, String tipoBusqueda, String terminoBusqueda,
                            ClienteServicio cliente, JsonObject usuarioData) {
        this.resultadosBusqueda = resultados;
        this.tipoBusqueda = tipoBusqueda;
        this.terminoBusqueda = terminoBusqueda;
        this.cliente = cliente;
        this.usuarioData = usuarioData;

        mostrarResultados();
    }

    /**
     * Metodo llamado al inicializar la vista para cargar fuentes personalizadas.
     */

    @FXML
    private void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoMono-Regular.ttf"), 12);
    }

    private void mostrarResultados() {
        resultsContainer.getChildren().clear();

        if (resultadosBusqueda.size() == 0) {
            Label noResults = new Label("No se encontraron resultados para '" + terminoBusqueda +
                    "' en " + tipoBusqueda.toLowerCase());
            noResults.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            resultsContainer.getChildren().add(noResults);
            return;
        }

        titleLabel.setText("Resultados de búsqueda (" + resultadosBusqueda.size() + " encontrados)");

        for (JsonElement elemento : resultadosBusqueda) {
            JsonObject contenido = elemento.getAsJsonObject();
            resultsContainer.getChildren().add(crearItemContenido(contenido));
        }
    }

    /**
     * Crea un nodo visual para representar un contenido en la lista de resultados.
     *
     * @param contenido Objeto JSON que representa el contenido.
     * @return Un nodo VBox que contiene los detalles del contenido.
     */

    private Node crearItemContenido(JsonObject contenido) {
        VBox item = new VBox(8);
        item.getStyleClass().add("contenido-item");

        HBox tituloBox = new HBox(5);
        tituloBox.setAlignment(Pos.CENTER_LEFT);

        String tipoContenido = contenido.get("tipo").getAsString();
        Label iconoTipo = new Label(obtenerIconoTipo(tipoContenido));
        iconoTipo.setStyle("-fx-font-size: 16px;");

        Label titulo = new Label(contenido.get("titulo").getAsString());
        titulo.getStyleClass().add("contenido-titulo");

        tituloBox.getChildren().addAll(iconoTipo, titulo);

        HBox metadatos = new HBox(10);
        metadatos.getStyleClass().add("contenido-metadatos");

        Label autor = crearMetadataLabel("👤 " + contenido.get("autor").getAsString());

        String fechaStr = "Fecha no disponible";
        if (contenido.has("fechaCreacion")) {
            try {
                fechaStr = contenido.get("fechaCreacion").getAsString();
            } catch (Exception e) {
                fechaStr = "Fecha no disponible";
            }
        }

        Label fecha = crearMetadataLabel("📅 " + fechaStr);
        Label tema = crearMetadataLabel("🏷 " + contenido.get("tema").getAsString());

        metadatos.getChildren().addAll(autor, fecha, tema);

        Text descripcion = new Text(contenido.get("descripcion").getAsString());
        descripcion.getStyleClass().add("descripcion-text");
        descripcion.setWrappingWidth(600);

        item.getChildren().addAll(tituloBox, metadatos, descripcion);

        item.setOnMouseEntered(e -> item.setStyle("-fx-border-color: #bdc3c7;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-border-color: #e0e0e0;"));
        item.setOnMouseClicked(e -> abrirDetalleContenido(contenido));

        return item;
    }

    /**
     * Abre una nueva ventana para mostrar los detalles del contenido seleccionado.
     *
     * @param contenido Objeto JSON que contiene los detalles del contenido.
     */

    private void abrirDetalleContenido(JsonObject contenido) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/contenido-layout.fxml"));
            Parent root = loader.load();

            ControladorContenido controlador = loader.getController();
            controlador.inicializar(contenido, cliente, usuarioData);

            Stage stage = new Stage();
            stage.setTitle("Detalles del Contenido");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Crea una etiqueta de metadatos con estilo personalizado.
     *
     * @param text Texto a mostrar en la etiqueta.
     * @return Una etiqueta configurada con el texto y estilo adecuado.
     */

    private Label crearMetadataLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metadata-label");
        return label;
    }

    /**
     * Obtiene un icono representativo según el tipo de contenido.
     *
     * @param tipo Tipo de contenido (ej. "Video", "Documento", etc.).
     * @return Un string con el icono correspondiente.
     */

    private String obtenerIconoTipo(String tipo) {
        switch(tipo.toUpperCase()) {
            case "VIDEO": return "🎬";
            case "DOCUMENTO": return "📄";
            case "ENLACE": return "🔗";
            case "IMAGEN": return "🖼";
            case "AUDIO": return "🎧";
            default: return "📌";
        }
    }

}