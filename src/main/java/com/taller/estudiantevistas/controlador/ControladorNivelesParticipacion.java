package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ControladorNivelesParticipacion {

    @FXML private VBox contenedorPrincipal;
    @FXML private Label lblTitulo;
    @FXML private BarChart<String, Number> graficoParticipacion;

    public void inicializar(JsonObject datosParticipacion) {
        configurarEstilos();
        cargarDatos(datosParticipacion);
    }

    private void configurarEstilos() {
        contenedorPrincipal.getStyleClass().add("panel-contenedor");
        lblTitulo.getStyleClass().add("contenido-titulo");
    }

    private void cargarDatos(JsonObject datos) {
        lblTitulo.setText("Niveles de Participación de Estudiantes");


        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setLabel("Estudiantes");
        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("Nivel de Participación");

        graficoParticipacion.setTitle("Participación en el Foro");
        graficoParticipacion.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Participación");

        JsonArray estudiantes = datos.getAsJsonArray("estudiantes");
        for (JsonElement estudiante : estudiantes) {
            JsonObject obj = estudiante.getAsJsonObject();
            String nombre = obj.get("nombre").getAsString();
            int participacion = obj.get("nivelParticipacion").getAsInt();

            series.getData().add(new XYChart.Data<>(nombre, participacion));
        }

        graficoParticipacion.getData().add(series);

        for (XYChart.Data<String, Number> data : series.getData()) {
            data.getNode().setStyle("-fx-bar-fill: #8e44ff;");
        }
    }
}