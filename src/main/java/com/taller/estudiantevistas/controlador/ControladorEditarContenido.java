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

    @FXML private TextField txtTitulo;
    @FXML private TextField txtAutor;
    @FXML private TextField txtTema;
    @FXML private ComboBox<TipoContenido> cbTipo;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtContenido;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private VBox contenedorPrincipal;

    private JsonObject contenidoOriginal;
    private Consumer<Boolean> callbackActualizacion;
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Variables para la conexión directa
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private String moderadorId;

    public void inicializar(JsonObject contenidoJson, String moderadorId, Consumer<Boolean> callback) {
        System.out.println("[DEBUG] Inicializando ControladorEditarContenido");
        System.out.println("[DEBUG] Contenido recibido: " + contenidoJson);
        System.out.println("[DEBUG] ID Moderador: " + moderadorId);

        this.contenidoOriginal = contenidoJson;
        this.moderadorId = moderadorId;
        this.callbackActualizacion = callback;

        // Configurar combobox de tipos
        System.out.println("[DEBUG] Configurando ComboBox de tipos");
        cbTipo.getItems().addAll(TipoContenido.values());

        // Cargar datos del contenido
        System.out.println("[DEBUG] Cargando datos del contenido en la UI");
        cargarDatosContenido();

        // Configurar eventos
        System.out.println("[DEBUG] Configurando eventos de los botones");
        configurarEventos();

        // Iniciar conexión
        System.out.println("[DEBUG] Estableciendo conexión con el servidor");
        conectarAlServidor();
    }

    private void conectarAlServidor() {
        try {
            String host = "localhost";
            int puerto = 1234;

            System.out.println("[DEBUG] Conectando a " + host + ":" + puerto);
            this.socket = new Socket(host, puerto);
            this.salida = new PrintWriter(socket.getOutputStream(), true);
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("[DEBUG] Conexión establecida con el servidor");
        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo conectar al servidor: " + e.getMessage());
            mostrarAlerta("Error de conexión", "No se pudo conectar al servidor: " + e.getMessage(), Alert.AlertType.ERROR);
            cerrarVentana();
        }
    }

    private void cargarDatosContenido() {
        System.out.println("[DEBUG] Cargando datos del contenido original a los campos de edición");
        try {
            System.out.println("[DEBUG] Cargando título: " + contenidoOriginal.get("titulo").getAsString());
            txtTitulo.setText(contenidoOriginal.get("titulo").getAsString());

            System.out.println("[DEBUG] Cargando autor: " + contenidoOriginal.get("autor").getAsString());
            txtAutor.setText(contenidoOriginal.get("autor").getAsString());

            System.out.println("[DEBUG] Cargando tema: " + contenidoOriginal.get("tema").getAsString());
            txtTema.setText(contenidoOriginal.get("tema").getAsString());

            // Manejar el tipo de contenido
            String tipoStr = contenidoOriginal.get("tipo").getAsString();
            System.out.println("[DEBUG] Cargando tipo: " + tipoStr);
            TipoContenido tipo = TipoContenido.valueOf(tipoStr);
            cbTipo.getSelectionModel().select(tipo);

            System.out.println("[DEBUG] Cargando descripción");
            txtDescripcion.setText(contenidoOriginal.get("descripcion").getAsString());

            // El campo "contenido" puede contener múltiples partes separadas por |
            String contenidoCompleto = contenidoOriginal.get("contenido").getAsString();
            System.out.println("[DEBUG] Contenido original completo: " + contenidoCompleto);

            if (contenidoCompleto.contains("|")) {
                System.out.println("[DEBUG] Dividiendo contenido por '|'");
                contenidoCompleto = contenidoCompleto.split("\\|")[0].trim();
            }

            System.out.println("[DEBUG] Contenido a mostrar: " + contenidoCompleto);
            txtContenido.setText(contenidoCompleto);

        } catch (Exception e) {
            System.err.println("[ERROR] Error al cargar datos del contenido: " + e.getMessage());
            mostrarAlerta("Error", "No se pudieron cargar los datos del contenido", Alert.AlertType.ERROR);
        }
    }

    private void configurarEventos() {
        System.out.println("[DEBUG] Configurando evento para botón Guardar");
        btnGuardar.setOnAction(e -> {
            System.out.println("[DEBUG] Botón Guardar clickeado");
            guardarCambios();
        });

        System.out.println("[DEBUG] Configurando evento para botón Cancelar");
        btnCancelar.setOnAction(e -> {
            System.out.println("[DEBUG] Botón Cancelar clickeado");
            cerrarVentana();
        });
    }

    private void guardarCambios() {
        System.out.println("[DEBUG] Iniciando proceso de guardar cambios");

        // Validar campos obligatorios
        if (txtTitulo.getText().isEmpty()) {
            System.out.println("[DEBUG] Validación fallida: título vacío");
        }
        if (txtAutor.getText().isEmpty()) {
            System.out.println("[DEBUG] Validación fallida: autor vacío");
        }
        if (cbTipo.getValue() == null) {
            System.out.println("[DEBUG] Validación fallida: tipo no seleccionado");
        }
        if (txtContenido.getText().isEmpty()) {
            System.out.println("[DEBUG] Validación fallida: contenido vacío");
        }

        if (txtTitulo.getText().isEmpty() || txtAutor.getText().isEmpty() ||
                cbTipo.getValue() == null || txtContenido.getText().isEmpty()) {
            System.out.println("[DEBUG] Validación fallida: campos obligatorios faltantes");
            mostrarAlerta("Error", "Todos los campos marcados con * son obligatorios", Alert.AlertType.ERROR);
            return;
        }

        System.out.println("[DEBUG] Todos los campos obligatorios están completos");

        // Crear objeto JSON con los cambios
        JsonObject contenidoActualizado = new JsonObject();
        contenidoActualizado.addProperty("id", contenidoOriginal.get("id").getAsString());
        contenidoActualizado.addProperty("titulo", txtTitulo.getText());
        contenidoActualizado.addProperty("autor", txtAutor.getText());
        contenidoActualizado.addProperty("tema", txtTema.getText());
        contenidoActualizado.addProperty("tipo", cbTipo.getValue().toString());
        contenidoActualizado.addProperty("descripcion", txtDescripcion.getText());
        contenidoActualizado.addProperty("contenido", txtContenido.getText());

        System.out.println("[DEBUG] Contenido actualizado: " + contenidoActualizado);

        // Mantener la fecha original
        contenidoActualizado.addProperty("fechaPublicacion",
                contenidoOriginal.get("fechaPublicacion").getAsString());

        // Enviar al servidor
        System.out.println("[DEBUG] Preparando solicitud para actualizar contenido");
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "ACTUALIZAR_CONTENIDO");

                        JsonObject datos = new JsonObject();
                        datos.add("contenido", contenidoActualizado);
                        datos.addProperty("moderadorId", moderadorId);
                        solicitud.add("datos", datos);

                        System.out.println("[DEBUG] Enviando solicitud al servidor: " + solicitud);
                        // Enviar solicitud
                        salida.println(solicitud.toString());
                        salida.flush();

                        // Recibir respuesta
                        System.out.println("[DEBUG] Esperando respuesta del servidor...");
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            System.err.println("[ERROR] El servidor no respondió");
                            throw new IOException("El servidor no respondió");
                        }

                        System.out.println("[DEBUG] Respuesta recibida: " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        System.err.println("[ERROR] Error de comunicación: " + e.getMessage());
                        throw new RuntimeException("Error de comunicación: " + e.getMessage(), e);
                    }
                },
                respuesta -> {
                    System.out.println("[DEBUG] Procesando respuesta del servidor");
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            System.out.println("[DEBUG] Actualización exitosa");
                            Platform.runLater(() -> {
                                mostrarAlerta("Éxito", "Contenido actualizado correctamente", Alert.AlertType.INFORMATION);
                                callbackActualizacion.accept(true);
                                cerrarVentana();
                            });
                        } else {
                            String mensajeError = jsonRespuesta.get("mensaje").getAsString();
                            System.err.println("[ERROR] Error en la actualización: " + mensajeError);
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR)
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error al procesar respuesta: " + e.getMessage());
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "Error al procesar respuesta del servidor", Alert.AlertType.ERROR)
                        );
                    }
                },
                "actualización de contenido"
        );
    }

    private void cerrarVentana() {
        System.out.println("[DEBUG] Cerrando ventana de edición");
        try {
            if (salida != null) {
                System.out.println("[DEBUG] Cerrando PrintWriter");
                salida.close();
            }
            if (entrada != null) {
                System.out.println("[DEBUG] Cerrando BufferedReader");
                entrada.close();
            }
            if (socket != null) {
                System.out.println("[DEBUG] Cerrando Socket");
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Error al cerrar conexión: " + e.getMessage());
        }

        Stage stage = (Stage) contenedorPrincipal.getScene().getWindow();
        stage.close();
        System.out.println("[DEBUG] Ventana cerrada exitosamente");
    }

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess, String contexto) {
        System.out.println("[DEBUG] Ejecutando tarea asíncrona: " + contexto);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                System.out.println("[DEBUG] Ejecutando tarea en segundo plano");
                return tarea.get();
            }
        };

        task.setOnSucceeded(e -> {
            System.out.println("[DEBUG] Tarea completada exitosamente");
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            System.err.println("[ERROR] Error en tarea asíncrona: " + task.getException().getMessage());
            Platform.runLater(() ->
                    mostrarAlerta("Error", "Error en " + contexto + ": " + task.getException().getMessage(), Alert.AlertType.ERROR)
            );
        });

        executor.execute(task);
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        System.out.println("[DEBUG] Mostrando alerta: " + titulo + " - " + mensaje);
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}