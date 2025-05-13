package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public class ControladorPerfil {

    @FXML private ImageView imgPerfil;
    @FXML private Button btnActualizarDatos;
    @FXML private Button btnVerContenidos;
    @FXML private Button btnVerSugerencias;
    @FXML private Button btnBuscarGrupos;
    @FXML private Button btnVerSolicitudes;
    @FXML private Button btnPublicarAyuda;
    @FXML private Button btnPublicarContenido;
    @FXML private ComboBox<String> comboGruposEstudio;

    @FXML
    public void initialize() {
        comboGruposEstudio.getItems().addAll("Grupo de Matemáticas", "Grupo de Programación", "Grupo de Inglés");

        btnActualizarDatos.setOnAction(e -> System.out.println("Actualizando datos..."));
        btnVerContenidos.setOnAction(e -> System.out.println("Viendo contenidos publicados..."));
        btnVerSugerencias.setOnAction(e -> System.out.println("Mostrando sugerencias..."));
        btnBuscarGrupos.setOnAction(e -> System.out.println("Buscando grupos de estudio..."));
        btnVerSolicitudes.setOnAction(e -> System.out.println("Viendo solicitudes activas..."));
        btnPublicarAyuda.setOnAction(e -> System.out.println("Publicando solicitud de ayuda..."));
        btnPublicarContenido.setOnAction(e -> System.out.println("Publicando contenido..."));
    }
}
