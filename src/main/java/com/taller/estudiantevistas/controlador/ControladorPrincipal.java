package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class ControladorPrincipal {
    // Campos para datos del usuario
    private JsonObject usuarioData;
    private ClienteServicio cliente;

    // Componentes de la barra superior
    @FXML private TextField campoBusqueda;
    @FXML private ComboBox<String> comboTipo;
    @FXML private Button btnBuscar, btnAjustes, btnNotificaciones, btnChat, btnPerfil, btnContacto;


    // ImageViews
    @FXML private ImageView imgLupa, imgAjustes, imgCampana, imgChat, imgPerfil, imgContacto;

    // Componentes del área central
    @FXML private Button btnRecargarContenidos, btnRecargarSolicitudes;
    @FXML private Pane panelContenidos, panelSolicitudes;

    public void inicializarConUsuario(String datosUsuario, ClienteServicio cliente) {
        try {
            this.usuarioData = JsonParser.parseString(datosUsuario).getAsJsonObject();
            this.cliente = cliente; // Guardar la conexión persistente
            cargarContenidosIniciales();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron cargar los datos del usuario", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }


    @FXML
    private void initialize() {
        configurarComboBox();
        configurarEventos();
        cargarImagenes();
    }

    private void cargarImagenes() {
        try {
            imgLupa.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/lupa.png")));
            imgAjustes.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/ajustes.png")));
            imgCampana.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/campana.png")));
            imgChat.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/chat.png")));
            imgPerfil.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/perfil.png")));
            imgContacto.setImage(new Image(getClass().getResourceAsStream("/com/taller/estudiantevistas/icons/contacto.png")));
        } catch (Exception e) {
            System.err.println("Error al cargar las imágenes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarComboBox() {
        comboTipo.getItems().addAll("Tema", "Autor", "Tipo");
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

    // Métodos de acción mejorados con datos de usuario
    private void buscarContenido() {
        String busqueda = campoBusqueda.getText();
        String tipo = comboTipo.getValue();

        // Usar ID de usuario para búsquedas personalizadas
        String userId = usuarioData != null ? usuarioData.get("id").getAsString() : "anonimo";

        System.out.println("Usuario " + userId + " buscando: " + busqueda + " - Tipo: " + tipo);
        // Aquí implementarías la lógica real de búsqueda con el servidor
    }

    private void recargarContenidos() {
        System.out.println("Recargando contenidos educativos para usuario: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
        // Implementar carga real desde servidor
    }

    private void recargarSolicitudes() {
        System.out.println("Recargando solicitudes de ayuda para usuario: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
        // Implementar carga real desde servidor
    }

    private void abrirAjustes() {
        System.out.println("Abriendo ajustes para: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
        // Implementar lógica para abrir ventana de ajustes
    }

    private void mostrarNotificaciones() {
        System.out.println("Mostrando notificaciones para: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
        // Implementar lógica para mostrar notificaciones
    }

    private void abrirChat() {
        System.out.println("Abriendo chat para: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
        // Implementar lógica para abrir chat
    }

    private void abrirPerfil() {
        if (usuarioData != null) {
            System.out.println("Abriendo perfil de: " + usuarioData.get("nombre").getAsString());
            // Aquí podrías abrir una ventana de perfil con los datos completos
        }
    }

    private void abrirContacto() {
        System.out.println("Abriendo contacto para: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
        // Implementar lógica para abrir contacto
    }

    private void cargarContenidosIniciales() {
        // Método para cargar contenidos al iniciar la pantalla
        if (usuarioData != null) {
            System.out.println("Cargando contenidos iniciales para " + usuarioData.get("nombre").getAsString());
            // Aquí implementarías la carga inicial desde el servidor
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}