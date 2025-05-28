package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ControladorSugerenciasPerfil {
    private static final Logger LOGGER = Logger.getLogger(ControladorSugerenciasPerfil.class.getName());

    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FXML private Pane panelSugerencias;
    private String userId;
    private ClienteServicio cliente;
    private BiConsumer<String, Consumer<JsonArray>> cargadorSugerencias;

    public void inicializar(String userId, ClienteServicio cliente, BiConsumer<String, Consumer<JsonArray>> cargadorSugerencias) {
        this.userId = userId;
        this.cliente = cliente;
        this.cargadorSugerencias = cargadorSugerencias;
        cargarSugerencias();
    }

    @FXML
    private void cargarSugerencias() {
        panelSugerencias.getChildren().clear();
        LOGGER.info("Iniciando carga de sugerencias para usuario: " + userId);

        if (cargadorSugerencias != null) {
            // Opción 1: Usar cargador personalizado
            cargadorSugerencias.accept(userId, sugerencias -> {
                Platform.runLater(() -> {
                    if (sugerencias == null || sugerencias.size() == 0) {
                        LOGGER.warning("No se recibieron sugerencias (cargador personalizado)");
                        mostrarSugerenciasEnPanel(crearMensajeInformacion(
                                "No hay sugerencias",
                                "No hay sugerencias de compañeros disponibles"));
                    } else {
                        LOGGER.info("Sugerencias recibidas: " + sugerencias.size());
                        mostrarSugerenciasEnPanel(sugerencias);
                    }
                });
            });
        } else {
            // Opción 2: Solicitud al servidor
            ejecutarTareaAsync(
                    () -> {
                        try {
                            LOGGER.info("Preparando solicitud de sugerencias...");
                            JsonObject solicitud = new JsonObject();
                            JsonObject datos = new JsonObject();
                            datos.addProperty("userId", userId);

                            solicitud.addProperty("tipo", "OBTENER_SUGERENCIAS");
                            solicitud.add("datos", datos);

                            // Debug: Mostrar solicitud que se enviará
                            LOGGER.fine("Enviando solicitud: " + solicitud.toString());

                            // Enviar solicitud
                            synchronized (cliente.getSalida()) {
                                cliente.getSalida().println(solicitud.toString());
                            }

                            // Esperar respuesta con timeout
                            String respuesta = null;
                            try {
                                respuesta = cliente.getEntrada().readLine();
                                LOGGER.fine("Respuesta recibida: " + respuesta);
                            } catch (IOException e) {
                                LOGGER.severe("Error al leer respuesta: " + e.getMessage());
                                throw new RuntimeException("Error de comunicación con el servidor", e);
                            }

                            if (respuesta == null || respuesta.isEmpty()) {
                                throw new RuntimeException("Respuesta vacía del servidor");
                            }

                            return JsonParser.parseString(respuesta).getAsJsonObject();
                        } catch (Exception e) {
                            LOGGER.severe("Error en la tarea asíncrona: " + e.getMessage());
                            throw e;
                        }
                    },
                    respuesta -> {
                        try {
                            LOGGER.info("Procesando respuesta del servidor...");
                            if (respuesta.get("exito").getAsBoolean()) {
                                JsonArray sugerencias = respuesta.getAsJsonArray("sugerencias");
                                LOGGER.info("Sugerencias recibidas del servidor: " + sugerencias.size());

                                Platform.runLater(() -> {
                                    if (sugerencias.size() == 0) {
                                        mostrarSugerenciasEnPanel(crearMensajeInformacion(
                                                "No hay sugerencias",
                                                "No encontramos compañeros con intereses similares"));
                                    } else {
                                        mostrarSugerenciasEnPanel(sugerencias);
                                    }
                                });
                            } else {
                                String mensajeError = respuesta.get("mensaje").getAsString();
                                LOGGER.warning("Error del servidor: " + mensajeError);
                                Platform.runLater(() ->
                                        mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR)
                                );
                            }
                        } catch (Exception e) {
                            LOGGER.severe("Error al procesar respuesta: " + e.getMessage());
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", "Error procesando respuesta del servidor", Alert.AlertType.ERROR)
                            );
                        }
                    },
                    error -> {
                        LOGGER.severe("Error en la carga de sugerencias: " + error.getMessage());
                        Platform.runLater(() -> {
                            mostrarSugerenciasEnPanel(crearMensajeError(error));
                            mostrarAlerta("Error",
                                    "No se pudieron cargar las sugerencias: " + error.getMessage(),
                                    Alert.AlertType.ERROR);
                        });
                    },
                    "carga de sugerencias"
            );
        }
    }

    private void mostrarSugerenciasEnPanel(JsonArray sugerencias) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(10));
        contenedor.setStyle("-fx-background-color: #f5f5f5;");
        contenedor.setMaxWidth(panelSugerencias.getWidth() - 20);

        if (esMensajeEspecial(sugerencias)) {
            JsonObject mensaje = sugerencias.get(0).getAsJsonObject();
            contenedor.getChildren().add(crearMensajeUI(
                    mensaje.get("titulo").getAsString(),
                    mensaje.get("detalle").getAsString(),
                    this::cargarSugerencias
            ));
        } else {
            for (JsonElement elemento : sugerencias) {
                JsonObject sugerencia = elemento.getAsJsonObject();
                contenedor.getChildren().add(crearItemSugerencia(sugerencia));
            }
        }

        ScrollPane scrollPane = crearScrollPane(contenedor);
        scrollPane.setPrefViewportWidth(panelSugerencias.getWidth() - 15);
        panelSugerencias.getChildren().clear();
        panelSugerencias.getChildren().add(scrollPane);
    }

    private Node crearItemSugerencia(JsonObject sugerencia) {
        VBox item = new VBox(10);
        item.getStyleClass().add("sugerencia-item");

        String nombre = sugerencia.has("nombre") ? sugerencia.get("nombre").getAsString() : "Compañero";
        String intereses = sugerencia.has("intereses") ? sugerencia.get("intereses").getAsString() : "No especificados";
        String grupo = sugerencia.has("grupo") ? sugerencia.get("grupo").getAsString() : "Sin grupo";

        Label nombreLabel = new Label(nombre);
        nombreLabel.getStyleClass().add("sugerencia-nombre");

        HBox interesesBox = new HBox(5);
        interesesBox.setAlignment(Pos.CENTER_LEFT);
        Label interesesIcon = new Label("🎯");
        Label interesesLabel = new Label("Intereses: " + intereses);
        interesesLabel.getStyleClass().add("sugerencia-detalle");
        interesesBox.getChildren().addAll(interesesIcon, interesesLabel);

        HBox grupoBox = new HBox(5);
        grupoBox.setAlignment(Pos.CENTER_LEFT);
        Label grupoIcon = new Label("👥");
        Label grupoLabel = new Label("Grupo: " + grupo);
        grupoLabel.getStyleClass().add("sugerencia-detalle");
        grupoBox.getChildren().addAll(grupoIcon, grupoLabel);

        Button btnContactar = new Button("Contactar");
        btnContactar.getStyleClass().add("btn-contactar");
        btnContactar.setOnAction(e -> contactarCompanero(sugerencia));

        item.getChildren().addAll(nombreLabel, interesesBox, grupoBox, btnContactar);
        return item;
    }

    private void contactarCompanero(JsonObject sugerencia) {
        try {
            String idCompanero = sugerencia.has("id") ? sugerencia.get("id").getAsString() : "";
            String nombreCompanero = sugerencia.has("nombre") ? sugerencia.get("nombre").getAsString() : "Compañero";


            mostrarAlerta("Contacto iniciado",
                    "Has iniciado contacto con " + nombreCompanero + "\nID: " + idCompanero,
                    Alert.AlertType.INFORMATION);



        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo iniciar el contacto: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private ScrollPane crearScrollPane(Node contenido) {
        ScrollPane scrollPane = new ScrollPane(contenido);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPadding(new Insets(0));
        scrollPane.setPrefViewportWidth(panelSugerencias.getWidth() - 15);
        scrollPane.getStyleClass().add("scroll-pane");
        return scrollPane;
    }

    private boolean esMensajeEspecial(JsonArray datos) {
        return datos.size() == 1 && datos.get(0).getAsJsonObject().has("esMensaje");
    }

    private JsonArray crearMensajeInformacion(String titulo, String detalle) {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("esMensaje", true);
        mensaje.addProperty("titulo", titulo);
        mensaje.addProperty("detalle", detalle);

        JsonArray respuesta = new JsonArray();
        respuesta.add(mensaje);
        return respuesta;
    }

    private JsonArray crearMensajeError(Throwable error) {
        return crearMensajeInformacion("Error", error.getMessage());
    }

    private Node crearMensajeUI(String titulo, String detalle, Runnable accionRecargar) {
        VBox cajaMensaje = new VBox(5);
        cajaMensaje.setAlignment(Pos.CENTER);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblDetalle = new Label(detalle);
        lblDetalle.setStyle("-fx-text-fill: #666;");

        Button btnRecargar = new Button("Intentar nuevamente");
        btnRecargar.setOnAction(e -> accionRecargar.run());

        cajaMensaje.getChildren().addAll(lblTitulo, lblDetalle, btnRecargar);
        return cajaMensaje;
    }

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess, Consumer<Throwable> onError, String contexto) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return tarea.get();
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            LOGGER.severe("Error en " + contexto + ": " + task.getException().getMessage());
            onError.accept(task.getException());
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

    private void abrirVentanaChat(String idCompanero, String nombreCompanero) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/chat.fxml"));
            Parent root = loader.load();



            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Chat con " + nombreCompanero);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir el chat: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}