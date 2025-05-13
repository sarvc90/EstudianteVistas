package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ControladorCrearPublicacion {

    @FXML private TextField tituloField;
    @FXML private TextField descripcionField;
    @FXML private TextField etiquetasField;
    @FXML private TextField enlaceField;
    @FXML private Button agregarArchivoBtn;

    @FXML
    public void initialize() {
        // Configuración inicial si es necesaria
        agregarArchivoBtn.setOnAction(event -> agregarArchivo());
    }

    @FXML
    private void publicar() {
        if (validarCampos()) {
            String titulo = tituloField.getText();
            String descripcion = descripcionField.getText();
            String etiquetas = etiquetasField.getText();
            String enlace = enlaceField.getText();

            System.out.println("Publicación creada:");
            System.out.println("Título: " + titulo);
            System.out.println("Descripción: " + descripcion);
            System.out.println("Etiquetas: " + etiquetas);
            System.out.println("Enlace: " + enlace);

            mostrarAlerta("Éxito", "Publicación creada correctamente");
            limpiarCampos();
        }
    }

    @FXML
    private void agregarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo");
        File file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            System.out.println("Archivo seleccionado: " + file.getAbsolutePath());
            mostrarAlerta("Archivo", "Archivo agregado: " + file.getName());
        }
    }

    private boolean validarCampos() {
        if (tituloField.getText().isEmpty()) {
            mostrarAlerta("Error", "El título es obligatorio");
            return false;
        }
        return true;
    }

    private void limpiarCampos() {
        tituloField.clear();
        descripcionField.clear();
        etiquetasField.clear();
        enlaceField.clear();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}