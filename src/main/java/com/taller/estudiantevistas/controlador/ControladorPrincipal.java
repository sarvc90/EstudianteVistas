// Solución 1: Modificar el ControladorPrincipal.java

package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class ControladorPrincipal {

    // Componentes de la barra superior
    @FXML private TextField campoBusqueda;
    @FXML private ComboBox<String> comboTipo;
    @FXML private Button btnBuscar, btnAjustes, btnNotificaciones, btnChat, btnPerfil, btnContacto;

    // ImageViews para las imágenes
    @FXML private ImageView imgLupa, imgAjustes, imgCampana, imgChat, imgPerfil, imgContacto;

    // Componentes del área central
    @FXML private Button btnRecargarContenidos, btnRecargarSolicitudes;
    @FXML private Pane panelContenidos, panelSolicitudes;

    @FXML
    private void initialize() {
        configurarComboBox();
        configurarEventos();
        cargarImagenes();
    }

    private void cargarImagenes() {
        // Cargar imágenes programáticamente
        try {
            // Opción 1: Cargar con getResource del ClassLoader
            imgLupa.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/lupa.png")));
            imgAjustes.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/ajustes.png")));
            imgCampana.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/campana.png")));
            imgChat.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/chat.png")));
            imgPerfil.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/perfil.png")));
            imgContacto.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/contacto.png")));

            // Si la opción 1 no funciona, prueba esta ruta relativa:
            // imgLupa.setImage(new Image(getClass().getResourceAsStream("../icons/lupa.png")));
        } catch (Exception e) {
            System.err.println("Error al cargar las imágenes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarComboBox() {
        comboTipo.getItems().addAll("Todos", "Matemáticas", "Ciencias", "Literatura", "Historia", "Programación");
        comboTipo.getSelectionModel().selectFirst();
    }

    private void configurarEventos() {
        btnBuscar.setOnAction(event -> buscarContenido());
        btnRecargarContenidos.setOnAction(event -> recargarContenidos());
        btnRecargarSolicitudes.setOnAction(event -> recargarSolicitudes());
        btnAjustes.setOnAction(event -> abrirAjustes());
        btnNotificaciones.setOnAction(event -> mostrarNotificaciones());
        btnChat.setOnAction(event -> abrirChat());
        btnPerfil.setOnAction(event -> abrirPerfil());
        btnContacto.setOnAction(event -> abrirContacto());
    }

    // Métodos de acción
    private void buscarContenido() {
        String busqueda = campoBusqueda.getText();
        String tipo = comboTipo.getValue();
        System.out.println("Buscando: " + busqueda + " - Tipo: " + tipo);
    }

    private void recargarContenidos() {
        System.out.println("Recargando contenidos educativos...");
    }

    private void recargarSolicitudes() {
        System.out.println("Recargando solicitudes de ayuda...");
    }

    private void abrirAjustes() {
        System.out.println("Abriendo ajustes...");
    }

    private void mostrarNotificaciones() {
        System.out.println("Mostrando notificaciones...");
    }

    private void abrirChat() {
        System.out.println("Abriendo chat...");
    }

    private void abrirPerfil() {
        System.out.println("Abriendo perfil...");
    }

    private void abrirContacto() {
        System.out.println("Abriendo contacto...");
    }
}