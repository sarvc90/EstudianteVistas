package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import com.taller.estudiantevistas.dto.TipoContenido;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ControladorEditarContenido {

    // Componentes UI
    @FXML private TextField txtTitulo;
    @FXML private TextField txtAutor;
    @FXML private TextField txtTema;
    @FXML private ComboBox<TipoContenido> cbTipo;
    @FXML private TextArea txtDescripcion;
    @FXML private TextArea txtContenido;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private VBox contenedorPrincipal;
    @FXML private Label lblErrores;

    // Datos y estado
    private JsonObject contenidoOriginal;
    private Consumer<Boolean> callbackActualizacion;
    private String moderadorId;

    // Conexión
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;

    // Executor para tareas asíncronas
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public void inicializar(JsonObject contenidoJson, String moderadorId, Consumer<Boolean> callback) {
        this.contenidoOriginal = contenidoJson;
        this.moderadorId = moderadorId;
        this.callbackActualizacion = callback;

        initUI();


        Platform.runLater(() -> {
            conectarAlServidor();
        });
    }

    private void initUI() {
        // Configurar ComboBox
        cbTipo.getItems().addAll(TipoContenido.values());

        // Cargar datos
        cargarDatosContenido();

        // Configurar eventos
        btnGuardar.setOnAction(e -> validarYGuardar());
        btnCancelar.setOnAction(e -> cerrarVentana());

        // Estilos iniciales
        lblErrores.setVisible(false);
    }

    private void cargarDatosContenido() {
        try {
            txtTitulo.setText(contenidoOriginal.get("titulo").getAsString());
            txtAutor.setText(contenidoOriginal.get("autor").getAsString());
            txtTema.setText(contenidoOriginal.get("tema").getAsString());

            TipoContenido tipo = TipoContenido.valueOf(
                    contenidoOriginal.get("tipo").getAsString());
            cbTipo.getSelectionModel().select(tipo);

            txtDescripcion.setText(contenidoOriginal.get("descripcion").getAsString());

            String contenido = contenidoOriginal.get("contenido").getAsString();
            if (contenido.contains("|")) {
                contenido = contenido.split("\\|")[0].trim();
            }
            txtContenido.setText(contenido);

        } catch (Exception e) {
            mostrarError("Error al cargar datos: " + e.getMessage());
        }
    }

    private void validarYGuardar() {
        lblErrores.setVisible(false);
        StringBuilder errores = new StringBuilder();

        if (txtTitulo.getText().trim().isEmpty()) {
            errores.append("• El título es obligatorio\n");
            txtTitulo.getStyleClass().add("campo-invalido");
        } else {
            txtTitulo.getStyleClass().remove("campo-invalido");
        }

        if (txtAutor.getText().trim().isEmpty()) {
            errores.append("• El autor es obligatorio\n");
            txtAutor.getStyleClass().add("campo-invalido");
        } else {
            txtAutor.getStyleClass().remove("campo-invalido");
        }

        if (cbTipo.getValue() == null) {
            errores.append("• Seleccione un tipo\n");
            cbTipo.getStyleClass().add("campo-invalido");
        } else {
            cbTipo.getStyleClass().remove("campo-invalido");
        }

        if (txtContenido.getText().trim().isEmpty()) {
            errores.append("• El contenido no puede estar vacío\n");
            txtContenido.getStyleClass().add("campo-invalido");
        } else {
            txtContenido.getStyleClass().remove("campo-invalido");
        }

        if (errores.length() > 0) {
            lblErrores.setText(errores.toString());
            lblErrores.setVisible(true);
            return;
        }

        guardarCambios();
    }

    private void guardarCambios() {
        if (socket == null || !socket.isConnected()) {
            mostrarError("No hay conexión con el servidor. Intente nuevamente.");
            conectarAlServidor();
            return;
        }

        try {
            // Construir el objeto de contenido actualizado
            JsonObject contenidoActualizado = new JsonObject();
            contenidoActualizado.addProperty("id", contenidoOriginal.get("id").getAsString());
            contenidoActualizado.addProperty("titulo", txtTitulo.getText().trim());
            contenidoActualizado.addProperty("autor", txtAutor.getText().trim());
            contenidoActualizado.addProperty("tema", txtTema.getText().trim());
            contenidoActualizado.addProperty("tipo", cbTipo.getValue().toString());
            contenidoActualizado.addProperty("descripcion", txtDescripcion.getText().trim());
            contenidoActualizado.addProperty("contenido", txtContenido.getText().trim());

            // Mantener la fecha original si existe
            if (contenidoOriginal.has("fechaCreacion") && !contenidoOriginal.get("fechaCreacion").isJsonNull()) {
                contenidoActualizado.addProperty("fecha", contenidoOriginal.get("fechaCreacion").getAsString());
            }

            // Construir la solicitud completa
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "ACTUALIZAR_CONTENIDO");

            JsonObject datosSolicitud = new JsonObject();
            datosSolicitud.add("contenido", contenidoActualizado); // Todos los datos del contenido
            datosSolicitud.addProperty("moderadorId", moderadorId);

            solicitud.add("datos", datosSolicitud);

            System.out.println("[DEBUG] Solicitud final: " + solicitud);

            ejecutarTareaAsync(
                    () -> {
                        try {
                            return enviarSolicitudActualizacion(solicitud);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    this::procesarRespuestaActualizacion,
                    "actualización de contenido"
            );

        } catch (Exception e) {
            mostrarError("Error al preparar la actualización: " + e.getMessage());
        }
    }

    private String enviarSolicitudActualizacion(JsonObject solicitudCompleta) throws IOException {
        if (salida == null || entrada == null) {
            throw new IOException("No hay conexión establecida con el servidor");
        }

        try {
            System.out.println("[DEBUG] Enviando solicitud completa: " + solicitudCompleta);
            salida.println(solicitudCompleta.toString());
            salida.flush();

            String respuesta = entrada.readLine();
            if (respuesta == null) {
                throw new IOException("El servidor no respondió");
            }
            System.out.println("[DEBUG] Respuesta recibida: " + respuesta);
            return respuesta;
        } catch (IOException e) {
            System.err.println("[ERROR] Error al enviar solicitud: " + e.getMessage());
            conectarAlServidor(); // Intenta reconectar
            throw e;
        }
    }

    private void procesarRespuestaActualizacion(String respuesta) {
        try {
            JsonObject json = JsonParser.parseString(respuesta).getAsJsonObject();
            if (json.get("exito").getAsBoolean()) {
                Platform.runLater(() -> {
                    mostrarAlerta("Éxito", "Contenido actualizado", Alert.AlertType.INFORMATION);
                    callbackActualizacion.accept(true);
                    cerrarVentana();
                });
            } else {
                Platform.runLater(() ->
                        mostrarError("Error del servidor: " + json.get("mensaje").getAsString()));
            }
        } catch (Exception e) {
            Platform.runLater(() ->
                    mostrarError("Error al procesar respuesta: " + e.getMessage()));
        }
    }

    private void conectarAlServidor() {
        new Thread(() -> {
            try {
                System.out.println("[DEBUG] Intentando conectar al servidor...");
                this.socket = new Socket("localhost", 12345);
                this.salida = new PrintWriter(socket.getOutputStream(), true);
                this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("[DEBUG] Conexión al servidor establecida");

                Platform.runLater(() -> {
                    btnGuardar.setDisable(false);
                    mostrarError(""); // Limpiar mensajes de error previos
                });

            } catch (IOException e) {
                System.err.println("[ERROR] Error de conexión: " + e.getMessage());
                Platform.runLater(() -> {
                    mostrarError("No se pudo conectar al servidor. Verifique que el servidor esté ejecutándose.");
                    btnGuardar.setDisable(true);

                    Button btnReintentar = new Button("Reintentar conexión");
                    btnReintentar.setOnAction(evt -> conectarAlServidor());
                    if (contenedorPrincipal.getChildren().size() > 2) {
                        contenedorPrincipal.getChildren().add(2, btnReintentar);
                    }
                });
            }
        }).start();
    }

    private void cerrarVentana() {
        try {
            if (salida != null) salida.close();
            if (entrada != null) entrada.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }

        // Cierre seguro de la ventana
        Platform.runLater(() -> {
            Stage stage = (Stage) contenedorPrincipal.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        });
    }

    private void mostrarError(String mensaje) {
        lblErrores.setText(mensaje);
        lblErrores.setVisible(true);
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess, String contexto) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception {
                return tarea.get();
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Platform.runLater(() ->
                    mostrarError("Error en " + contexto + ": " + task.getException().getMessage()));
        });

        executor.execute(task);
    }
}