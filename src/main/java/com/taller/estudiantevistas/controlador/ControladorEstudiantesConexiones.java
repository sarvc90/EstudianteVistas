package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.logging.Logger;

public class ControladorEstudiantesConexiones {

    private static final Logger LOGGER = Logger.getLogger(ControladorEstudiantesConexiones.class.getName());

    @FXML private TableView<Estudiante> tablaEstudiantes;
    @FXML private TableColumn<Estudiante, String> colNombre;
    @FXML private TableColumn<Estudiante, Integer> colConexiones;
    @FXML private TableColumn<Estudiante, String> colEstado;
    @FXML private TableColumn<Estudiante, String> colUltimaActividad;
    @FXML private VBox panelPrincipal;
    @FXML private TextField campoBusqueda;

    private ObservableList<Estudiante> listaEstudiantes = FXCollections.observableArrayList();

    public void inicializar(JsonObject datosConexiones) {
        configurarTabla();
        cargarDatos(datosConexiones);
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colConexiones.setCellValueFactory(new PropertyValueFactory<>("conexiones"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colUltimaActividad.setCellValueFactory(new PropertyValueFactory<>("ultimaActividad"));

        tablaEstudiantes.setRowFactory(tv -> {
            TableRow<Estudiante> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    mostrarDetalleEstudiante(row.getItem());
                }
            });
            return row;
        });

        campoBusqueda.textProperty().addListener((obs, oldVal, newVal) -> {
            filtrarEstudiantes();
        });
    }

    private void cargarDatos(JsonObject datosConexiones) {
        listaEstudiantes.clear();

        if (datosConexiones.has("estudiantes") && datosConexiones.get("estudiantes").isJsonArray()) {
            JsonArray estudiantes = datosConexiones.getAsJsonArray("estudiantes");

            for (JsonElement estudiante : estudiantes) {
                JsonObject obj = estudiante.getAsJsonObject();

                try {
                    String id = obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsString() : "sin-id";
                    String nombre = obj.has("nombre") && !obj.get("nombre").isJsonNull() ? obj.get("nombre").getAsString() : "desconocido";
                    int conexiones = obj.has("conexiones") && !obj.get("conexiones").isJsonNull() ? obj.get("conexiones").getAsInt() : 0;
                    String estado = obj.has("estado") && !obj.get("estado").isJsonNull() ? obj.get("estado").getAsString() : "desconocido";
                    String ultimaActividad = obj.has("ultimaActividad") && !obj.get("ultimaActividad").isJsonNull()
                            ? obj.get("ultimaActividad").getAsString()
                            : "no disponible";

                    String intereses = obj.has("intereses") && !obj.get("intereses").isJsonNull()
                            ? obj.get("intereses").getAsString()
                            : "No especificado";

                    Estudiante e = new Estudiante(id, nombre, conexiones, estado, ultimaActividad, intereses);
                    listaEstudiantes.add(e);

                } catch (Exception ex) {
                    LOGGER.severe("Error al procesar estudiante: " + obj + "\n" + ex.getMessage());
                }
            }

            tablaEstudiantes.setItems(listaEstudiantes);
        } else {
            LOGGER.warning("No se encontraron estudiantes en los datos recibidos");
        }
    }


    private void filtrarEstudiantes() {
        String busqueda = campoBusqueda.getText().toLowerCase();

        ObservableList<Estudiante> listaFiltrada = FXCollections.observableArrayList();

        for (Estudiante estudiante : listaEstudiantes) {
            if (estudiante.getNombre().toLowerCase().contains(busqueda) ||
                    estudiante.getIntereses().toLowerCase().contains(busqueda)) {
                listaFiltrada.add(estudiante);
            }
        }

        tablaEstudiantes.setItems(listaFiltrada);
    }

    private void mostrarDetalleEstudiante(Estudiante estudiante) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle del estudiante");
        alert.setHeaderText(estudiante.getNombre());

        Label detalles = new Label(
                "Conexiones: " + estudiante.getConexiones() + "\n" +
                        "Estado: " + estudiante.getEstado() + "\n" +
                        "Última actividad: " + estudiante.getUltimaActividad() + "\n" +
                        "Intereses: " + estudiante.getIntereses()
        );
        detalles.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        alert.getDialogPane().setContent(detalles);
        alert.getDialogPane().setPrefSize(400, 200);
        alert.showAndWait();
    }

    public static class Estudiante {
        private final String id;
        private final String nombre;
        private final int conexiones;
        private final String estado;
        private final String ultimaActividad;
        private final String intereses;

        public Estudiante(String id, String nombre, int conexiones, String estado, String ultimaActividad, String intereses) {
            this.id = id;
            this.nombre = nombre;
            this.conexiones = conexiones;
            this.estado = estado;
            this.ultimaActividad = ultimaActividad;
            this.intereses = intereses;
        }

        // Getters
        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public int getConexiones() { return conexiones; }
        public String getEstado() { return estado; }
        public String getUltimaActividad() { return ultimaActividad; }
        public String getIntereses() { return intereses; }
    }
}