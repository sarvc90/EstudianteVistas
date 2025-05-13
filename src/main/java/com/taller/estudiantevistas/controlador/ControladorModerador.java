package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public class ControladorModerador {

    @FXML private ImageView imgPerfil;
    @FXML private Label lblNombres, lblApellidos, lblCedula, lblCorreo, lblIntereses, lblContrasena;
    @FXML private Button btnActualizarDatos, btnVerUsuarios, btnVerContenidos;
    @FXML private Button btnVerGrafo, btnFuncionalidadGrafo, btnTablaContenidos;
    @FXML private Button btnEstudiantesConexiones, btnNivelesParticipacion;

    @FXML
    public void initialize() {
        btnActualizarDatos.setOnAction(e -> System.out.println("Actualizando datos..."));
        btnVerUsuarios.setOnAction(e -> System.out.println("Viendo usuarios..."));
        btnVerContenidos.setOnAction(e -> System.out.println("Viendo contenidos..."));
        btnVerGrafo.setOnAction(e -> System.out.println("Mostrando grafo..."));
        btnFuncionalidadGrafo.setOnAction(e -> System.out.println("Mostrando funcionalidad de grafo..."));
        btnTablaContenidos.setOnAction(e -> System.out.println("Mostrando tabla de contenidos..."));
        btnEstudiantesConexiones.setOnAction(e -> System.out.println("Mostrando estudiantes con más conexiones..."));
        btnNivelesParticipacion.setOnAction(e -> System.out.println("Mostrando niveles de participación..."));
    }
}
