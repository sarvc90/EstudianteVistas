package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ControladorModerador {

    private static final Logger LOGGER = Logger.getLogger(ControladorModerador.class.getName());
    private JsonObject datosUsuario;
    private ClienteServicio cliente;
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FXML private ImageView imgPerfil;
    @FXML private Label lblNombres, lblApellidos, lblCedula, lblCorreo, lblIntereses, lblContrasena;
    @FXML private Button btnActualizarDatos, btnVerUsuarios, btnVerContenidos;
    @FXML private Button btnVerGrafo, btnFuncionalidadGrafo, btnTablaContenidos;
    @FXML private Button btnEstudiantesConexiones, btnNivelesParticipacion;

    @FXML
    public void initialize() {
        btnActualizarDatos.setOnAction(e -> manejarActualizarDatos());
        btnVerUsuarios.setOnAction(e -> manejarVerUsuarios());
        btnVerContenidos.setOnAction(e -> manejarVerContenidos());
        btnVerGrafo.setOnAction(e -> manejarVerGrafo());
        btnFuncionalidadGrafo.setOnAction(e -> manejarFuncionalidadGrafo());
        btnTablaContenidos.setOnAction(e -> manejarTablaContenidos());
        btnEstudiantesConexiones.setOnAction(e -> manejarEstudiantesConexiones());
        btnNivelesParticipacion.setOnAction(e -> manejarNivelesParticipacion());
    }

    public void inicializar(JsonObject datosUsuario, ClienteServicio cliente) {
        this.datosUsuario = datosUsuario;
        this.cliente = cliente;

        // Obtener datos completos del usuario desde el servidor
        obtenerDatosUsuarioCompletos(datosUsuario.get("id").getAsString());
    }

    private void obtenerDatosUsuarioCompletos(String userId) {
        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_DATOS_MODERADOR");
                    solicitud.addProperty("userId", userId);
                    cliente.getSalida().println(solicitud.toString());
                    try {
                        return cliente.getEntrada().readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                respuesta -> {
                    JsonObject datosCompletos = JsonParser.parseString(respuesta).getAsJsonObject();
                    Platform.runLater(() -> actualizarUI(datosCompletos));
                },
                "obtención de datos de moderador"
        );
    }

    private void actualizarUI(JsonObject datosUsuario) {
        LOGGER.info("Inicializando vista de moderador para: " + datosUsuario.get("nombres").getAsString());

        lblNombres.setText(datosUsuario.get("nombres").getAsString());
        lblApellidos.setText(datosUsuario.get("apellidos").getAsString());
        lblCedula.setText(datosUsuario.get("cedula").getAsString());
        lblCorreo.setText(datosUsuario.get("correo").getAsString());
        lblIntereses.setText(datosUsuario.get("intereses").getAsString());
        lblContrasena.setText("********"); // por seguridad
    }

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess, String contexto) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return tarea.get();
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            LOGGER.severe("Error en " + contexto + ": " + task.getException().getMessage());
            Platform.runLater(() -> mostrarAlerta("Error", "Error al " + contexto, Alert.AlertType.ERROR));
        });

        executor.execute(task);
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void manejarActualizarDatos() {
        LOGGER.info("Actualizando datos...");
        // Implementar lógica para actualizar datos
    }

    private void manejarVerUsuarios() {
        LOGGER.info("Viendo usuarios...");
        // Implementar lógica para obtener y mostrar usuarios
    }

    private void manejarVerContenidos() {
        LOGGER.info("Viendo contenidos...");
        // Implementar lógica para obtener y mostrar contenidos
    }

    private void manejarVerGrafo() {
        LOGGER.info("Mostrando grafo...");
        // Implementar lógica para obtener y mostrar grafo
    }

    private void manejarFuncionalidadGrafo() {
        LOGGER.info("Mostrando funcionalidad de grafo...");
        // Implementar lógica para funcionalidad de grafo
    }

    private void manejarTablaContenidos() {
        LOGGER.info("Mostrando tabla de contenidos...");
        // Implementar lógica para tabla de contenidos
    }

    private void manejarEstudiantesConexiones() {
        LOGGER.info("Mostrando estudiantes con más conexiones...");
        // Implementar lógica para estudiantes con conexiones
    }

    private void manejarNivelesParticipacion() {
        LOGGER.info("Mostrando niveles de participación...");
        // Implementar lógica para niveles de participación
    }
}