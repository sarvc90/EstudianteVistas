package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class ControladorContenido {

    @FXML
    private void agregarComentario() {
        mostrarAlerta("Comentario", "Funcionalidad para agregar comentario");
    }

    @FXML
    private void agregarValoracion() {
        mostrarAlerta("Valoración", "Funcionalidad para agregar valoración");
    }

    @FXML
    private void verValoracionPromedio() {
        mostrarAlerta("Valoración Promedio", "Mostrar valoración promedio");
    }

    @FXML
    private void verComentarios() {
        mostrarAlerta("Comentarios", "Mostrar todos los comentarios");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}