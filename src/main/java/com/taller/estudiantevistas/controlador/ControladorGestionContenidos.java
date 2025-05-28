package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ControladorGestionContenidos {

    @FXML private TableView<ContenidoTabla> tablaContenidos;
    @FXML private TableColumn<ContenidoTabla, String> colIdContenido, colTitulo, colAutor, colTema, colTipo, colFecha;
    @FXML private Button btnEditar, btnEliminar;

    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private String moderadorId;

    public void inicializar(String moderadorId) {
        this.moderadorId = moderadorId;
        conectarAlServidor();
        configurarTablaContenidos();
        cargarContenidos();
    }

    private void conectarAlServidor() {
        try {
            String host = "localhost";
            int puerto = 12345;
            System.out.println("[DEBUG] Conectando al servidor en " + host + ":" + puerto);
            this.socket = new Socket(host, puerto);
            this.salida = new PrintWriter(socket.getOutputStream(), true);
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("[DEBUG] Conexión establecida exitosamente");
        } catch (Exception e) {
            System.err.println("[ERROR] Error al conectar al servidor: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo conectar al servidor: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void configurarTablaContenidos() {
        System.out.println("[DEBUG] Configurando tabla de contenidos");
        colIdContenido.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colTema.setCellValueFactory(new PropertyValueFactory<>("tema"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        tablaContenidos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean seleccionado = newVal != null;
            btnEditar.setDisable(!seleccionado);
            btnEliminar.setDisable(!seleccionado);
            System.out.println("[DEBUG] Contenido seleccionado: " + (seleccionado ? newVal.getTitulo() : "ninguno"));
        });
    }

    @FXML
    private void cargarContenidos() {
        System.out.println("[DEBUG] Solicitando lista de contenidos al servidor");
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_CONTENIDOS"); // Tipo correcto

                        JsonObject datos = new JsonObject();
                        solicitud.add("datos", datos); // Estructura requerida por el servidor

                        String jsonRequest = solicitud.toString();
                        System.out.println("[DEBUG JSON] Enviando solicitud: " + jsonRequest);

                        // Validación adicional
                        if (!jsonRequest.contains("OBTENER_CONTENIDOS")) {
                            throw new RuntimeException("Tipo de solicitud incorrecto generado");
                        }

                        salida.println(jsonRequest);

                        System.out.println("[DEBUG] Esperando respuesta...");
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            throw new IOException("El servidor no respondió");
                        }
                        System.out.println("[DEBUG] Respuesta recibida: " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación: " + e.getMessage());
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            System.out.println("[DEBUG] Carga de contenidos exitosa");
                            JsonArray contenidos = jsonRespuesta.getAsJsonArray("contenidos");
                            System.out.println("[DEBUG] Contenidos recibidos: " + contenidos.size());
                            Platform.runLater(() -> actualizarTablaContenidos(contenidos));
                        } else {
                            String error = jsonRespuesta.get("mensaje").getAsString();
                            System.err.println("[ERROR] Error al cargar contenidos: " + error);
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", error, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error al procesar respuesta: " + e.getMessage());
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "Error al procesar respuesta del servidor", Alert.AlertType.ERROR));
                    }
                },
                "carga de contenidos"
        );
    }

    private void actualizarTablaContenidos(JsonArray contenidos) {
        System.out.println("[DEBUG] Actualizando tabla con " + contenidos.size() + " contenidos");
        ObservableList<ContenidoTabla> datos = FXCollections.observableArrayList();

        Set<String> idsMostrados = new HashSet<>();

        contenidos.forEach(contenido -> {
            try {
                JsonObject c = contenido.getAsJsonObject();
                String idContenido = c.get("id").getAsString();

                if (!idsMostrados.contains(idContenido)) {
                    // Manejo seguro de la fecha
                    String fechaStr = "";
                    if (c.has("fechaCreacion") && !c.get("fechaCreacion").isJsonNull()) {
                        fechaStr = c.get("fechaCreacion").getAsString();
                    }

                    datos.add(new ContenidoTabla(
                            idContenido,
                            c.get("titulo").getAsString(),
                            c.get("autor").getAsString(),
                            c.get("tema").getAsString(),
                            c.get("tipo").getAsString(),
                            fechaStr
                    ));
                    idsMostrados.add(idContenido);
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Error procesando contenido: " + e.getMessage());
            }
        });

        tablaContenidos.setItems(datos);
    }

    @FXML
    private void manejarEditar() {
        System.out.println("[DEBUG] Iniciando manejarEditar");

        ContenidoTabla contenido = tablaContenidos.getSelectionModel().getSelectedItem();
        if (contenido == null) {
            System.out.println("[DEBUG] No hay contenido seleccionado");
            mostrarAlerta("Selección requerida", "Por favor seleccione un contenido de la tabla", Alert.AlertType.WARNING);
            return;
        }

        System.out.println("[DEBUG] Contenido seleccionado para editar - ID: " + contenido.getId());

        // Verificar conexión y reconectar si es necesario
        if (salida == null || entrada == null || socket == null || socket.isClosed()) {
            System.out.println("[DEBUG] Reconectando al servidor...");
            conectarAlServidor();
            if (salida == null || entrada == null) {
                mostrarAlerta("Error", "No se pudo establecer conexión con el servidor", Alert.AlertType.ERROR);
                return;
            }
        }

        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "OBTENER_CONTENIDO_COMPLETO");

        JsonObject datos = new JsonObject();
        datos.addProperty("contenidoId", contenido.getId());
        datos.addProperty("moderadorId", this.moderadorId);
        solicitud.add("datos", datos);

        System.out.println("[DEBUG JSON] Enviando solicitud de edición: " + solicitud);

        ejecutarTareaAsync(
                () -> {
                    try {
                        System.out.println("[DEBUG] Enviando solicitud...");
                        salida.println(solicitud.toString());
                        salida.flush();

                        System.out.println("[DEBUG] Esperando respuesta...");
                        String respuesta = entrada.readLine();

                        if (respuesta == null) {
                            throw new IOException("El servidor no respondió");
                        }

                        System.out.println("[DEBUG] Respuesta recibida: " + respuesta);
                        return respuesta;
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error en comunicación: " + e.getMessage());
                        throw new RuntimeException("Error al comunicarse con el servidor", e);
                    }
                },
                respuesta -> {
                    try {
                        System.out.println("[DEBUG] Procesando respuesta...");
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

                        if (!jsonRespuesta.get("exito").getAsBoolean()) {
                            String error = jsonRespuesta.get("mensaje").getAsString();
                            System.err.println("[ERROR] Servidor reportó error: " + error);
                            Platform.runLater(() ->
                                    mostrarAlerta("Error del servidor", error, Alert.AlertType.ERROR));
                            return;
                        }

                        JsonObject contenidoCompleto = jsonRespuesta.getAsJsonObject("contenido");
                        System.out.println("[DEBUG] Contenido recibido: " + contenidoCompleto);

                        Platform.runLater(() -> {
                            try {
                                abrirVistaEdicion(contenidoCompleto);
                            } catch (Exception e) {
                                System.err.println("[ERROR] Error al abrir editor: " + e.getMessage());
                                mostrarAlerta("Error", "No se pudo abrir el editor: " + e.getMessage(), Alert.AlertType.ERROR);
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error procesando respuesta: " + e.getMessage());
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "Respuesta inválida del servidor: " + e.getMessage(), Alert.AlertType.ERROR));
                    }
                },
                "edición de contenido"
        );
    }

    private void abrirVistaEdicion(JsonObject contenidoCompleto) {
        System.out.println("[DEBUG] Iniciando abrirVistaEdicion");

        try {
            // 1. Obtener la URL del FXML
            URL fxmlUrl = getClass().getResource("/com/taller/estudiantevistas/fxml/editar_contenido.fxml");
            if (fxmlUrl == null) {
                throw new IOException("No se encontró el archivo FXML en la ruta especificada");
            }
            System.out.println("[DEBUG] URL del FXML: " + fxmlUrl);

            // 2. Crear el FXMLLoader
            FXMLLoader loader = new FXMLLoader(fxmlUrl);

            // 3. Cargar manualmente el controlador si es necesario
            loader.setControllerFactory(clazz -> {
                try {
                    ControladorEditarContenido controller = new ControladorEditarContenido();
                    System.out.println("[DEBUG] Controlador creado manualmente");
                    return controller;
                } catch (Exception e) {
                    System.err.println("[ERROR] No se pudo crear el controlador: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            });

            // 4. Cargar la vista
            Parent root = loader.load();
            System.out.println("[DEBUG] FXML cargado exitosamente");

            // 5. Obtener el controlador
            ControladorEditarContenido controlador = loader.getController();
            if (controlador == null) {
                throw new RuntimeException("El controlador no se pudo inicializar");
            }
            System.out.println("[DEBUG] Controlador obtenido: " + controlador.getClass().getName());

            // 6. Inicializar el controlador
            controlador.inicializar(
                    contenidoCompleto,
                    this.moderadorId,
                    exito -> {
                        if (exito) {
                            Platform.runLater(this::cargarContenidos);
                        }
                    }
            );

            // 7. Mostrar la ventana
            Stage stage = new Stage();
            stage.setTitle("Editando: " + contenidoCompleto.get("titulo").getAsString());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            System.err.println("[ERROR] Error al cargar FXML: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo cargar la ventana de edición", Alert.AlertType.ERROR);
        } catch (Exception e) {
            System.err.println("[ERROR] Error inesperado: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "Error al abrir el editor: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void manejarEliminarContenido() {
        ContenidoTabla contenido = tablaContenidos.getSelectionModel().getSelectedItem();
        if (contenido == null) {
            mostrarAlerta("Error", "Seleccione un contenido", Alert.AlertType.ERROR);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("Eliminar contenido: " + contenido.getTitulo());
        confirmacion.setContentText("¿Está seguro de eliminar este contenido permanentemente?");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                eliminarContenido(contenido.getId());
            }
        });
    }

    // Método mejorado para manejar la eliminación
    private void eliminarContenido(String id) {
        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "ELIMINAR_CONTENIDO");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("contenidoId", id);
                    datos.addProperty("moderadorId", moderadorId);
                    solicitud.add("datos", datos);

                    System.out.println("[DEBUG] Enviando solicitud de eliminación: " + solicitud);
                    salida.println(solicitud.toString());

                    String respuesta = null;
                    try {
                        respuesta = entrada.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (respuesta == null) {
                        try {
                            throw new IOException("El servidor no respondió");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return respuesta;
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> {
                                mostrarAlerta("Éxito", "Contenido eliminado correctamente", Alert.AlertType.INFORMATION);
                                cargarContenidos(); // Recargar la tabla después de eliminar
                            });
                        } else {
                            String error = jsonRespuesta.get("mensaje").getAsString();
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", error, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "Error al procesar respuesta del servidor", Alert.AlertType.ERROR));
                    }
                },
                "eliminación de contenido"
        );
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
            Platform.runLater(() ->
                    mostrarAlerta("Error", "Error en " + contexto + ": " + task.getException().getMessage(), Alert.AlertType.ERROR)
            );
        });

        executor.execute(task);
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Platform.runLater(() -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    public static class ContenidoTabla {
        private final String id;
        private final String titulo;
        private final String autor;
        private final String tema;
        private final String tipo;
        private final String fecha;

        public ContenidoTabla(String id, String titulo, String autor, String tema, String tipo, String fecha) {
            this.id = id;
            this.titulo = titulo;
            this.autor = autor;
            this.tema = tema;
            this.tipo = tipo;
            this.fecha = fecha;
        }

        public String getId() { return id; }
        public String getTitulo() { return titulo; }
        public String getAutor() { return autor; }
        public String getTema() { return tema; }
        public String getTipo() { return tipo; }
        public String getFecha() { return fecha; }
    }
}