package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

public class ControladorTablaContenidos implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ControladorTablaContenidos.class.getName());

    @FXML private TableView<Contenido> tablaContenidos;
    @FXML private TableColumn<Contenido, String> colTitulo;
    @FXML private TableColumn<Contenido, String> colAutor;
    @FXML private TableColumn<Contenido, String> colFecha;
    @FXML private TableColumn<Contenido, Integer> colLikes;
    @FXML private TableColumn<Contenido, Integer> colComentarios;
    @FXML private VBox panelPrincipal;

    private ObservableList<Contenido> listaContenidos = FXCollections.observableArrayList();

    private JsonObject datosContenidos;

    public void inicializar(JsonObject datosContenidos) {
        this.datosContenidos = datosContenidos;
        cargarDatos();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
        URL cssURL = getClass().getResource("/com/taller/estudiantevistas/css/reporte.css");
        if (cssURL != null && panelPrincipal != null) {
            panelPrincipal.getStylesheets().add(cssURL.toExternalForm());
        } else {
            System.err.println("No se encontró el archivo reporte.css o panelPrincipal es null");
        }
    }


    private void configurarTabla() {
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colComentarios.setCellValueFactory(new PropertyValueFactory<>("valoraciones"));

        tablaContenidos.setPlaceholder(new Label("No hay contenidos con valoraciones disponibles."));
    }

    private void cargarDatos() {
        listaContenidos.clear();

        if (datosContenidos.has("contenidos") && datosContenidos.get("contenidos").isJsonArray()) {
            JsonArray contenidos = datosContenidos.getAsJsonArray("contenidos");

            Map<String, Contenido> mapaPorTitulo = new LinkedHashMap<>();

            for (JsonElement contenido : contenidos) {
                JsonObject obj = contenido.getAsJsonObject();

                String titulo = obj.has("titulo") && !obj.get("titulo").isJsonNull()
                        ? obj.get("titulo").getAsString()
                        : "sin título";

                if (mapaPorTitulo.containsKey(titulo)) continue;

                String id = obj.has("id") && !obj.get("id").isJsonNull()
                        ? obj.get("id").getAsString()
                        : "sin-id";

                String autor = obj.has("autor") && !obj.get("autor").isJsonNull()
                        ? obj.get("autor").getAsString()
                        : "desconocido";

                String fecha = obj.has("fecha") && !obj.get("fecha").isJsonNull()
                        ? obj.get("fecha").getAsString()
                        : "sin fecha";

                int totalValoraciones = 0;

                if (obj.has("valoraciones") && obj.get("valoraciones").isJsonArray()) {
                    for (JsonElement valoracion : obj.getAsJsonArray("valoraciones")) {
                        if (valoracion.isJsonObject()) {
                            totalValoraciones += 1;
                        }
                    }
                }

                mapaPorTitulo.put(titulo, new Contenido(id, titulo, autor, fecha, totalValoraciones));
            }

            List<Contenido> listaUnica = new ArrayList<>(mapaPorTitulo.values());
            listaUnica.sort((a, b) -> Integer.compare(b.getValoraciones(), a.getValoraciones()));

            List<Contenido> top5 = listaUnica.size() > 5 ? listaUnica.subList(0, 5) : listaUnica;

            listaContenidos.addAll(top5);
            tablaContenidos.setItems(listaContenidos);
        } else {
            LOGGER.warning("No se encontraron contenidos en los datos recibidos");
        }
    }




    public static class Contenido {
        private final String id;
        private final String titulo;
        private final String autor;
        private final String fecha;
        private final int valoraciones;

        public Contenido(String id, String titulo, String autor, String fecha, int valoraciones) {
            this.id = id;
            this.titulo = titulo;
            this.autor = autor;
            this.fecha = fecha;
            this.valoraciones = valoraciones;
        }

        public String getId() { return id; }
        public String getTitulo() { return titulo; }
        public String getAutor() { return autor; }
        public String getFecha() { return fecha; }
        public int getValoraciones() { return valoraciones; }
    }
}