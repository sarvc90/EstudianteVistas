package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

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

public class ControladorSolicitudesPerfil {
    private static final Logger LOGGER = Logger.getLogger(ControladorSolicitudesPerfil.class.getName());

    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FXML private Pane panelSolicitudes;
    private String userId;
    private ClienteServicio cliente;
    private BiConsumer<String, Consumer<JsonArray>> cargadorSolicitudes;

    public void inicializar(String userId, ClienteServicio cliente, BiConsumer<String, Consumer<JsonArray>> cargadorSolicitudes) {
        this.userId = userId;
        this.cliente = cliente;
        this.cargadorSolicitudes = cargadorSolicitudes;
        cargarSolicitudesUsuario();
    }

    @FXML
    private void cargarSolicitudesUsuario() {
        panelSolicitudes.getChildren().clear();

        if (cargadorSolicitudes != null) {
            cargadorSolicitudes.accept(userId, solicitudes -> {
                Platform.runLater(() -> {
                    if (solicitudes == null || solicitudes.size() == 0) {
                        mostrarSolicitudesEnPanel(crearMensajeInformacion(
                                "No hay solicitudes",
                                "No has creado ninguna solicitud aún"));
                    } else {
                        mostrarSolicitudesEnPanel(solicitudes);
                    }
                });
            });
        } else {
            ejecutarTareaAsync(
                    () -> {
                        JsonObject solicitud = new JsonObject();
                        JsonObject datos = new JsonObject();
                        datos.addProperty("userId", userId);

                        solicitud.addProperty("tipo", "OBTENER_SOLICITUDES_USUARIO");
                        solicitud.add("datos", datos);

                        cliente.getSalida().println(solicitud.toString());
                        String respuesta = null;
                        try {
                            respuesta = cliente.getEntrada().readLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return JsonParser.parseString(respuesta).getAsJsonObject();
                    },
                    respuesta -> {
                        if (respuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() ->
                                    mostrarSolicitudesEnPanel(respuesta.getAsJsonArray("solicitudes"))
                            );
                        } else {
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR)
                            );
                        }
                    },
                    error -> Platform.runLater(() -> {
                        mostrarSolicitudesEnPanel(crearMensajeError(error));
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR);
                    }),
                    "carga de solicitudes del usuario"
            );
        }
    }

    private JsonArray filtrarDuplicados(JsonArray array) {
        Set<String> idsUnicos = new HashSet<>();
        JsonArray resultado = new JsonArray();

        for (JsonElement elemento : array) {
            JsonObject solicitud = elemento.getAsJsonObject();
            String id = solicitud.has("id") ? solicitud.get("id").getAsString() :
                    (solicitud.has("fecha") ? solicitud.get("fecha").getAsString() : null);

            if (id == null || !idsUnicos.contains(id)) {
                resultado.add(solicitud);
                if (id != null) {
                    idsUnicos.add(id);
                }
            }
        }
        return resultado;
    }

    private void mostrarSolicitudesEnPanel(JsonArray solicitudes) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(10));
        contenedor.setStyle("-fx-background-color: #f5f5f5;");
        contenedor.setMaxWidth(panelSolicitudes.getWidth() - 20);

        if (esMensajeEspecial(solicitudes)) {
            JsonObject mensaje = solicitudes.get(0).getAsJsonObject();
            contenedor.getChildren().add(crearMensajeUI(
                    mensaje.get("titulo").getAsString(),
                    mensaje.get("detalle").getAsString(),
                    this::cargarSolicitudesUsuario
            ));
        } else {
            Set<String> idsMostrados = new HashSet<>();
            for (JsonElement elemento : solicitudes) {
                JsonObject solicitud = elemento.getAsJsonObject();
                String id = solicitud.has("id") ? solicitud.get("id").getAsString() :
                        solicitud.has("fecha") ? solicitud.get("fecha").getAsString() : null;

                if (id == null || !idsMostrados.contains(id)) {
                    contenedor.getChildren().add(crearItemSolicitud(solicitud));
                    if (id != null) {
                        idsMostrados.add(id);
                    }
                }
            }
        }

        ScrollPane scrollPane = crearScrollPane(contenedor);
        scrollPane.setPrefViewportWidth(panelSolicitudes.getWidth() - 15);
        panelSolicitudes.getChildren().clear();
        panelSolicitudes.getChildren().add(scrollPane);
    }

    private Node crearItemSolicitud(JsonObject solicitud) {
        VBox item = new VBox(10);
        item.getStyleClass().add("solicitud-item");

        // Safe field access with null checks
        String tema = solicitud.has("tema") ? solicitud.get("tema").getAsString() : "Sin título";
        String descripcionText = solicitud.has("descripcion") ? solicitud.get("descripcion").getAsString() : "Sin descripción";
        String fechaStr = solicitud.has("fecha") ? formatFechaContenido(solicitud.get("fecha").getAsString()) : "Fecha desconocida";
        String urgencia = solicitud.has("urgencia") ? solicitud.get("urgencia").getAsString() : "MEDIA";
        String estado = solicitud.has("estado") ? solicitud.get("estado").getAsString() : "PENDIENTE";

        // Título
        Label temaLabel = new Label(tema);
        temaLabel.getStyleClass().add("solicitud-titulo");

        // Estados y urgencia
        HBox estadosBox = new HBox(10);
        estadosBox.getStyleClass().add("solicitud-estados");
        estadosBox.setAlignment(Pos.CENTER_LEFT);

        Label urgenciaLabel = new Label("🔺 " + urgencia);
        urgenciaLabel.getStyleClass().add("urgencia-" + urgencia.toLowerCase());

        Label estadoLabel = new Label("◉ " + estado);
        estadoLabel.getStyleClass().add("estado-" + estado.toLowerCase());

        estadosBox.getChildren().addAll(urgenciaLabel, estadoLabel);

        // Descripción
        TextArea descripcion = new TextArea(descripcionText);
        descripcion.getStyleClass().add("descripcion-text");
        descripcion.setEditable(false);
        descripcion.setWrapText(true);
        descripcion.setPrefRowCount(3);
        descripcion.setFocusTraversable(false);

        // Footer
        HBox footer = new HBox(15);
        footer.getStyleClass().add("solicitud-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        Label fechaLabel = new Label("📅 " + fechaStr);
        fechaLabel.getStyleClass().add("metadata-label");

        footer.getChildren().addAll(fechaLabel);

        item.getChildren().addAll(temaLabel, estadosBox, descripcion, footer);
        return item;
    }

    private String formatFechaContenido(String fechaStr) {
        try {
            SimpleDateFormat formatoOriginal = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date fecha = formatoOriginal.parse(fechaStr);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fecha);
        } catch (Exception e) {
            return fechaStr; // Si hay error, devolver el formato original
        }
    }

    private ScrollPane crearScrollPane(Node contenido) {
        ScrollPane scrollPane = new ScrollPane(contenido);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPadding(new Insets(0));
        scrollPane.setPrefViewportWidth(panelSolicitudes.getWidth() - 15);
        scrollPane.getStyleClass().add("scroll-pane");
        return scrollPane;
    }

    // Métodos utilitarios
    private String formatFecha(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(millis));
    }

    private Label crearMetadataLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metadata-label");
        return label;
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
}