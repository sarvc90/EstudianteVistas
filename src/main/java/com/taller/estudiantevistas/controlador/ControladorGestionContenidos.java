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
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ControladorGestionContenidos {

    @FXML private TableView<ContenidoTabla> tablaContenidos;
    @FXML private TableColumn<ContenidoTabla, String> colTitulo, colAutor, colTipo;
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
        configurarTabla();
        cargarContenidos();
    }

    private void conectarAlServidor() {
        try {
            String host = "localhost";
            int puerto = 1234;

            this.socket = new Socket(host, puerto);
            this.salida = new PrintWriter(socket.getOutputStream(), true);
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo conectar al servidor: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void configurarTabla() {
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));

        tablaContenidos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean seleccionado = newVal != null;
            btnEditar.setDisable(!seleccionado);
            btnEliminar.setDisable(!seleccionado);
        });
    }

    private void cargarContenidos() {
        if (socket == null || socket.isClosed()) {
            conectarAlServidor();
            if (socket == null || socket.isClosed()) {
                mostrarAlerta("Error", "No hay conexión con el servidor", Alert.AlertType.ERROR);
                return;
            }
        }

        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_TODOS_CONTENIDOS");
                        salida.println(solicitud.toString());
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            throw new IOException("El servidor cerró la conexión");
                        }
                        return respuesta;
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación: " + e.getMessage(), e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (!jsonRespuesta.has("exito")) {
                            throw new RuntimeException("Respuesta del servidor inválida");
                        }

                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            if (!jsonRespuesta.has("contenidos")) {
                                throw new RuntimeException("No se recibieron contenidos");
                            }
                            JsonArray contenidos = jsonRespuesta.getAsJsonArray("contenidos");
                            Platform.runLater(() -> actualizarTabla(contenidos));
                        } else {
                            String mensaje = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() :
                                    "Error desconocido del servidor";
                            throw new RuntimeException(mensaje);
                        }
                    } catch (JsonSyntaxException e) {
                        throw new RuntimeException("Error al procesar respuesta del servidor", e);
                    }
                },
                "carga de contenidos"
        );
    }

    private void actualizarTabla(JsonArray contenidos) {
        try {
            ObservableList<ContenidoTabla> contenidosList = FXCollections.observableArrayList();

            for (JsonElement contenido : contenidos) {
                JsonObject cont = contenido.getAsJsonObject();
                if (!cont.has("titulo") || !cont.has("autor") || !cont.has("tipo")) {
                    continue;
                }
                contenidosList.add(new ContenidoTabla(
                        cont.get("titulo").getAsString(),
                        cont.get("autor").getAsString(),
                        cont.get("tipo").getAsString()
                ));
            }

            Platform.runLater(() -> tablaContenidos.setItems(contenidosList));
        } catch (Exception e) {
            Platform.runLater(() ->
                    mostrarAlerta("Error", "Error al mostrar contenidos: " + e.getMessage(), Alert.AlertType.ERROR)
            );
        }
    }

    @FXML
    private void manejarEditar() {
        ContenidoTabla contenido = tablaContenidos.getSelectionModel().getSelectedItem();
        if (contenido == null) return;

        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_CONTENIDO_COMPLETO");
                        solicitud.addProperty("contenidoId", contenido.getTitulo());
                        salida.println(solicitud.toString());
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            throw new IOException("El servidor cerró la conexión");
                        }
                        return respuesta;
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación: " + e.getMessage(), e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (!jsonRespuesta.has("exito")) {
                            throw new RuntimeException("Respuesta del servidor inválida");
                        }

                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            if (!jsonRespuesta.has("contenido")) {
                                throw new RuntimeException("No se recibió el contenido completo");
                            }
                            JsonObject contenidoCompleto = jsonRespuesta.getAsJsonObject("contenido");
                            Platform.runLater(() -> abrirVistaEdicion(contenidoCompleto));
                        } else {
                            String mensaje = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() :
                                    "Error al obtener contenido";
                            throw new RuntimeException(mensaje);
                        }
                    } catch (JsonSyntaxException e) {
                        throw new RuntimeException("Error al procesar respuesta del servidor", e);
                    }
                },
                "obtención de contenido"
        );
    }

    private void abrirVistaEdicion(JsonObject contenidoCompleto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/editar_contenido.fxml"));
            Parent root = loader.load();

            ControladorEditarContenido controlador = loader.getController();
            controlador.inicializar(contenidoCompleto, moderadorId, (exito) -> {
                if (exito) {
                    this.cargarContenidos();
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Editar Contenido");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo abrir la vista de edición: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void manejarEliminar() {
        ContenidoTabla contenido = tablaContenidos.getSelectionModel().getSelectedItem();
        if (contenido == null) return;

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Eliminación");
        confirmacion.setHeaderText("Eliminar contenido " + contenido.getTitulo());
        confirmacion.setContentText("¿Está seguro de eliminar este contenido permanentemente?");

        Optional<ButtonType> result = confirmacion.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            eliminarContenido(contenido.getTitulo());
        }
    }

    private void eliminarContenido(String titulo) {
        if (socket == null || socket.isClosed()) {
            mostrarAlerta("Error", "No hay conexión con el servidor", Alert.AlertType.ERROR);
            return;
        }

        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "ELIMINAR_CONTENIDO");

                        JsonObject datos = new JsonObject();
                        datos.addProperty("contenidoId", titulo);
                        datos.addProperty("moderadorId", moderadorId);
                        solicitud.add("datos", datos);

                        salida.println(solicitud.toString());
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            throw new IOException("El servidor cerró la conexión");
                        }
                        return respuesta;
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación: " + e.getMessage(), e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (!jsonRespuesta.has("exito")) {
                            throw new RuntimeException("Respuesta del servidor inválida");
                        }

                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> {
                                mostrarAlerta("Éxito", "Contenido eliminado correctamente", Alert.AlertType.INFORMATION);
                                cargarContenidos();
                            });
                        } else {
                            String mensaje = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() :
                                    "Error al eliminar contenido";
                            throw new RuntimeException(mensaje);
                        }
                    } catch (JsonSyntaxException e) {
                        throw new RuntimeException("Error al procesar respuesta del servidor", e);
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
        private final String titulo;
        private final String autor;
        private final String tipo;

        public ContenidoTabla(String titulo, String autor, String tipo) {
            this.titulo = titulo;
            this.autor = autor;
            this.tipo = tipo;
        }

        public String getTitulo() { return titulo; }
        public String getAutor() { return autor; }
        public String getTipo() { return tipo; }
    }
}