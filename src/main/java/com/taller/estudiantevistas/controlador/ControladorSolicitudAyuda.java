package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

public class ControladorSolicitudAyuda {

    @FXML private TextField tituloField;
    @FXML private TextField temaField;
    @FXML private TextField contenidoField;

    @FXML
    private void mostrarOpcionesUrgencia() {
        // Implementar lógica para seleccionar urgencia
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Nivel de Urgencia");
        alert.setHeaderText(null);
        alert.setContentText("Aquí irían las opciones de urgencia");
        alert.showAndWait();
    }

    @FXML
    private void publicarSolicitud() {
        if (validarCampos()) {
            String titulo = tituloField.getText().trim();
            String tema = temaField.getText().trim();
            String contenido = contenidoField.getText().trim();

            System.out.println("\n--- Nueva Solicitud de Ayuda ---");
            System.out.println("Título: " + titulo);
            System.out.println("Tema: " + tema);
            System.out.println("Contenido: " + contenido);

            mostrarAlerta("Éxito", "Solicitud publicada correctamente");
            limpiarCampos();
        }
    }

    private boolean validarCampos() {
        if (tituloField.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El título es obligatorio");
            return false;
        }
        if (temaField.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El tema es obligatorio");
            return false;
        }
        return true;
    }

    private void limpiarCampos() {
        tituloField.clear();
        temaField.clear();
        contenidoField.clear();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}