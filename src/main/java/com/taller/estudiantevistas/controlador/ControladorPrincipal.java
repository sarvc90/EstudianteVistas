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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControladorPrincipal {
    private static final Logger LOGGER = Logger.getLogger(ControladorPrincipal.class.getName());

    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private JsonObject usuarioData;
    private ClienteServicio cliente;

    @FXML private TextField campoBusqueda;
    @FXML private ComboBox<String> comboTipo;
    @FXML private Button btnBuscar, btnAjustes, btnNotificaciones, btnChat, btnPerfil, btnContacto;
    @FXML private ImageView imgLupa, imgAjustes, imgCampana, imgChat, imgPerfil, imgContacto, imgRecargar;
    @FXML private Button btnRecargarContenidos, btnRecargarSolicitudes, btnNuevaSolicitud;
    @FXML private Pane panelContenidos, panelSolicitudes;

    public interface ActualizacionListener {
        void onContenidosActualizados(JsonArray contenidos);
        void onSolicitudesActualizadas(JsonArray solicitudes);
    }
    private final List<ActualizacionListener> listeners = new ArrayList<>();

    public void inicializarConUsuario(JsonObject usuarioData, ClienteServicio cliente) {
        Objects.requireNonNull(usuarioData, "Datos de usuario no pueden ser nulos");
        Objects.requireNonNull(cliente, "ClienteServicio no puede ser nulo");

        this.usuarioData = usuarioData;
        this.cliente = cliente;
        Platform.runLater(this::cargarContenidosIniciales);
    }

    @FXML
    private void initialize() {
        configurarComboBox();
        configurarEventos();
        cargarImagenes();
    }

    private void cargarImagenes() {
        try {
            imgLupa.setImage(loadImage("/com/taller/estudiantevistas/icons/lupa.png"));
            imgAjustes.setImage(loadImage("/com/taller/estudiantevistas/icons/ajustes.png"));
            imgCampana.setImage(loadImage("/com/taller/estudiantevistas/icons/campana.png"));
            imgChat.setImage(loadImage("/com/taller/estudiantevistas/icons/chat.png"));
            imgPerfil.setImage(loadImage("/com/taller/estudiantevistas/icons/perfil.png"));
            imgContacto.setImage(loadImage("/com/taller/estudiantevistas/icons/contacto.png"));
            imgRecargar.setImage(loadImage("/com/taller/estudiantevistas/icons/recargar.png"));
        } catch (Exception e) {
            manejarError("cargar imágenes", e);
        }
    }

    private Image loadImage(String path) throws IOException {
        return new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(path),
                "No se pudo cargar imagen: " + path
        ));
    }

    private void configurarComboBox() {
        comboTipo.getItems().addAll("Tema", "Autor", "Tipo");
        comboTipo.getSelectionModel().selectFirst();
    }

    private void configurarEventos() {
        btnBuscar.setOnAction(event -> buscarContenido());
        btnRecargarContenidos.setOnAction(event -> recargarContenidos());
        btnRecargarSolicitudes.setOnAction(event -> recargarSolicitudes());
        btnNuevaSolicitud.setOnAction(event -> mostrarVistaSolicitudAyuda());
        btnAjustes.setOnAction(event -> abrirAjustes());
        btnNotificaciones.setOnAction(event -> mostrarNotificaciones());
        btnChat.setOnAction(event -> abrirChat());
        btnPerfil.setOnAction(event -> abrirPerfil());
        btnContacto.setOnAction(event -> abrirContacto());
    }

    private void buscarContenido() {
        String busqueda = campoBusqueda.getText().trim();
        String tipoBusqueda = comboTipo.getValue();

        if (busqueda.isEmpty()) {
            mostrarAlerta("Búsqueda vacía", "Por favor ingrese un término de búsqueda", Alert.AlertType.WARNING);
            return;
        }

        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "BUSCAR_CONTENIDO");

                        JsonObject datos = new JsonObject();
                        datos.addProperty("criterio", tipoBusqueda);
                        datos.addProperty("busqueda", busqueda);
                        datos.addProperty("userId", usuarioData.get("id").getAsString());

                        solicitud.add("datos", datos);

                        cliente.getSalida().println(solicitud.toString());
                        String respuesta = cliente.getEntrada().readLine();
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

                        if (!jsonRespuesta.get("exito").getAsBoolean()) {
                            throw new RuntimeException(jsonRespuesta.get("mensaje").getAsString());
                        }

                        return jsonRespuesta.getAsJsonArray("resultados");
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación: " + e.getMessage());
                    } catch (JsonParseException e) {
                        throw new RuntimeException("Respuesta del servidor no válida");
                    }
                },
                resultados -> Platform.runLater(() -> {
                    mostrarContenidosEnPanel(resultados, panelContenidos);
                    notificarListeners("contenidos", resultados);
                }),
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR)
                ),
                "búsqueda de contenidos"
        );
    }

    // ==================== MÉTODOS DE CARGA DE CONTENIDOS ====================

    public void cargarContenidosParaPerfil(Consumer<JsonArray> callback) {
        if (usuarioData != null && usuarioData.has("id")) {
            ejecutarTareaAsync(
                    () -> {
                        try {
                            return obtenerContenidosCompletos(usuarioData.get("id").getAsString());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    callback::accept,
                    error -> mostrarAlerta("Error", "No se pudieron cargar los contenidos", Alert.AlertType.ERROR),
                    "carga de contenidos para perfil"
            );
        }
    }

    private JsonArray obtenerContenidosCompletos(String userId) throws IOException {
        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "OBTENER_CONTENIDOS_COMPLETOS");

        JsonObject datos = new JsonObject();
        datos.addProperty("userId", userId);
        solicitud.add("datos", datos);

        cliente.getSalida().println(solicitud.toString());
        String respuesta = cliente.getEntrada().readLine();
        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

        if (!jsonRespuesta.get("exito").getAsBoolean()) {
            throw new IOException(jsonRespuesta.get("mensaje").getAsString());
        }

        return jsonRespuesta.getAsJsonArray("contenidos");
    }

    @FXML
    private void recargarContenidos() {
        if (usuarioData != null && usuarioData.has("id")) {
            ejecutarTareaAsync(
                    () -> {
                        try {
                            JsonArray contenidos = cliente.obtenerContenidosEducativos(usuarioData.get("id").getAsString());

                            if (contenidos == null || contenidos.size() == 0) {
                                // Crear respuesta con mensaje amigable
                                JsonObject mensaje = new JsonObject();
                                mensaje.addProperty("esMensaje", true);
                                mensaje.addProperty("titulo", "Sin contenidos");
                                mensaje.addProperty("detalle", "No se encontraron contenidos para tu usuario");

                                JsonArray respuesta = new JsonArray();
                                respuesta.add(mensaje);
                                return respuesta;
                            }
                            return contenidos;
                        } catch (IOException e) {
                            throw new RuntimeException("Error de conexión: " + e.getMessage());
                        }
                    },
                    respuesta -> Platform.runLater(() -> {
                        mostrarContenidosEnPanel(respuesta, panelContenidos);
                        notificarListeners("contenidos", respuesta);
                    }),
                    error -> Platform.runLater(() -> {
                        // Crear mensaje de error para mostrar
                        JsonObject mensajeError = new JsonObject();
                        mensajeError.addProperty("esMensaje", true);
                        mensajeError.addProperty("titulo", "Error");
                        mensajeError.addProperty("detalle", error.getMessage());

                        JsonArray arrayError = new JsonArray();
                        arrayError.add(mensajeError);

                        mostrarContenidosEnPanel(arrayError, panelContenidos);
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR);
                    }),
                    "recarga de contenidos"
            );
        }
    }

    @FXML
    private void recargarSolicitudes() {
        if (usuarioData != null && usuarioData.has("id")) {
            ejecutarTareaAsync(
                    () -> {
                        try {
                            JsonArray solicitudes = cliente.obtenerSolicitudesAyuda(usuarioData.get("id").getAsString());

                            if (solicitudes.size() == 0) {
                                // Crear respuesta con mensaje amigable
                                JsonObject mensaje = new JsonObject();
                                mensaje.addProperty("esMensaje", true);
                                mensaje.addProperty("titulo", "No hay solicitudes");
                                mensaje.addProperty("detalle", "No se encontraron solicitudes pendientes");

                                JsonArray respuesta = new JsonArray();
                                respuesta.add(mensaje);
                                return respuesta;
                            }
                            return solicitudes;
                        } catch (RuntimeException e) {
                            throw new RuntimeException("Error al obtener solicitudes: " + e.getMessage());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    respuesta -> Platform.runLater(() -> {
                        mostrarSolicitudesEnPanel(respuesta, panelSolicitudes);
                        notificarListeners("solicitudes", respuesta);
                    }),
                    error -> Platform.runLater(() -> {
                        // Crear mensaje de error para mostrar
                        JsonObject mensajeError = new JsonObject();
                        mensajeError.addProperty("esMensaje", true);
                        mensajeError.addProperty("titulo", "Error");
                        mensajeError.addProperty("detalle", error.getMessage());

                        JsonArray arrayError = new JsonArray();
                        arrayError.add(mensajeError);

                        mostrarSolicitudesEnPanel(arrayError, panelSolicitudes);
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR);
                    }),
                    "recarga de solicitudes"
            );
        }
    }

    // ==================== MÉTODOS DE VISUALIZACIÓN ====================

    private void mostrarContenidosEnPanel(JsonArray contenidos, Pane panel) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(10));
        contenedor.setStyle("-fx-background-color: #f5f5f5;");

        if (contenidos.size() == 0 ||
                (contenidos.size() == 1 && contenidos.get(0).getAsJsonObject().has("esMensaje"))) {

            // Mostrar mensaje especial
            JsonObject mensajeObj = contenidos.size() > 0 ?
                    contenidos.get(0).getAsJsonObject() : null;

            String titulo = mensajeObj != null ?
                    mensajeObj.get("titulo").getAsString() : "Sin contenidos";
            String detalle = mensajeObj != null ?
                    mensajeObj.get("detalle").getAsString() : "No hay contenidos disponibles";

            VBox cajaMensaje = new VBox(5);
            cajaMensaje.setAlignment(Pos.CENTER);

            Label lblTitulo = new Label(titulo);
            lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            Label lblDetalle = new Label(detalle);
            lblDetalle.setStyle("-fx-text-fill: #666;");

            Button btnRecargar = new Button("Intentar nuevamente");
            btnRecargar.setOnAction(e -> recargarContenidos());

            cajaMensaje.getChildren().addAll(lblTitulo, lblDetalle, btnRecargar);
            contenedor.getChildren().add(cajaMensaje);
            contenedor.setAlignment(Pos.CENTER);
        } else {
            // Mostrar contenidos normales
            for (JsonElement elemento : contenidos) {
                try {
                    JsonObject contenido = elemento.getAsJsonObject();
                    if (!contenido.has("esMensaje")) {
                        Node item = crearItemContenido(contenido);
                        contenedor.getChildren().add(item);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Error al procesar contenido: " + e.getMessage());
                }
            }
        }

        ScrollPane scrollPane = new ScrollPane(contenedor);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        panel.getChildren().clear();
        panel.getChildren().add(scrollPane);
    }

    private Node crearItemContenido(JsonObject contenido) {
        VBox item = new VBox(10);
        item.getStyleClass().add("contenido-item");
        item.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1; -fx-padding: 10;");

        // Título
        Label titulo = new Label(contenido.get("titulo").getAsString());
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Detalles (autor y tema)
        HBox detalles = new HBox(10);
        detalles.getChildren().addAll(
                new Label("Autor: " + contenido.get("autor").getAsString()),
                new Label("Tema: " + contenido.get("tema").getAsString())
        );
        detalles.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");

        item.getChildren().addAll(titulo, detalles);

        // Descripción (si existe)
        if (contenido.has("descripcion") && !contenido.get("descripcion").getAsString().isEmpty()) {
            Text descripcion = new Text(contenido.get("descripcion").getAsString());
            descripcion.setWrappingWidth(panelContenidos.getWidth() - 30);
            descripcion.setStyle("-fx-font-size: 12px;");
            item.getChildren().add(descripcion);
        }

        return item;
    }

    private Node crearItemContenidoCompleto(JsonObject contenido) {
        VBox item = new VBox(10);
        item.getStyleClass().add("contenido-detallado");

        Label titulo = new Label(contenido.get("titulo").getAsString());
        titulo.getStyleClass().add("titulo-contenido");

        HBox metaInfo = new HBox(15);
        metaInfo.getChildren().addAll(
                new Label("Autor: " + contenido.get("autor").getAsString()),
                new Label("Tema: " + contenido.get("tema").getAsString()),
                new Label("Tipo: " + contenido.get("tipo").getAsString())
        );

        TextArea descripcion = new TextArea(contenido.get("descripcion").getAsString());
        descripcion.setEditable(false);
        descripcion.setWrapText(true);

        // Sección de valoraciones
        if (contenido.has("valoraciones")) {
            VBox valoracionesBox = new VBox(5);
            Label valoracionesLabel = new Label("Valoraciones:");
            valoracionesBox.getChildren().add(valoracionesLabel);

            JsonArray valoraciones = contenido.getAsJsonArray("valoraciones");
            valoraciones.forEach(v -> {
                JsonObject valoracion = v.getAsJsonObject();
                HBox valoracionItem = new HBox(10);
                valoracionItem.getChildren().addAll(
                        new Label("★".repeat(valoracion.get("valor").getAsInt())),
                        new Label(valoracion.get("comentario").getAsString())
                );
                valoracionesBox.getChildren().add(valoracionItem);
            });
            item.getChildren().add(valoracionesBox);
        }

        item.getChildren().addAll(titulo, metaInfo, descripcion);
        return item;
    }

    private void mostrarSolicitudesEnPanel(JsonArray solicitudes, Pane panel) {
        mostrarContenidoGenerico(
                solicitudes,
                panel,
                solicitud -> {
                    // Asegurarse de que los campos existan antes de acceder a ellos
                    String tema = solicitud.has("tema") ? solicitud.get("tema").getAsString() : "Sin tema";
                    String descripcion = solicitud.has("descripcion") ? solicitud.get("descripcion").getAsString() : "";
                    String urgencia = solicitud.has("urgencia") ? solicitud.get("urgencia").getAsString() : "MEDIA";
                    String estado = solicitud.has("estado") ? solicitud.get("estado").getAsString() : "PENDIENTE";
                    long fecha = solicitud.has("fecha") ? solicitud.get("fecha").getAsLong() : System.currentTimeMillis();

                    String solicitante = "Anónimo";
                    if (solicitud.has("solicitanteNombre")) {
                        solicitante = solicitud.get("solicitanteNombre").getAsString();
                    } else if (solicitud.has("solicitanteId")) {
                        solicitante = "ID: " + solicitud.get("solicitanteId").getAsString().substring(0, 6);
                    }

                    return crearItemSolicitud(tema, descripcion, urgencia, estado, fecha, solicitante);
                }
        );
    }

    private Node crearItemSolicitud(String tema, String descripcion, String urgencia,
                                    String estado, long fechaMillis, String solicitante) {
        VBox item = new VBox(8);
        item.getStyleClass().add("solicitud-item");

        Label temaLabel = new Label(tema);
        temaLabel.getStyleClass().add("solicitud-titulo");

        HBox detalles = new HBox(10);
        Label urgenciaLabel = new Label("Urgencia: " + urgencia);
        urgenciaLabel.getStyleClass().add("urgencia-" + urgencia.toLowerCase());

        Label estadoLabel = new Label("Estado: " + estado);
        estadoLabel.getStyleClass().add("estado-" + estado.toLowerCase().replace("_", ""));

        detalles.getChildren().addAll(urgenciaLabel, estadoLabel);

        TextArea descripcionArea = new TextArea(descripcion);
        descripcionArea.getStyleClass().add("descripcion-text");
        descripcionArea.setEditable(false);

        HBox footer = new HBox(10);
        Label fechaLabel = new Label("Fecha: " + formatFecha(fechaMillis));
        fechaLabel.getStyleClass().add("info-text");

        Label solicitanteLabel = new Label("Por: " + solicitante);
        solicitanteLabel.getStyleClass().add("info-text");

        footer.getChildren().addAll(fechaLabel, solicitanteLabel);

        item.getChildren().addAll(temaLabel, detalles, descripcionArea, footer);
        return item;
    }

    private void mostrarContenidoGenerico(JsonArray datos, Pane panel, Function<JsonObject, Node> factory) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(10));
        contenedor.setStyle("-fx-background-color: #f5f5f5;");

        if (datos.size() == 0) {
            contenedor.getChildren().add(crearMensajeVacio("No hay datos disponibles"));
        } else {
            datos.forEach(item ->
                    contenedor.getChildren().add(factory.apply(item.getAsJsonObject()))
            );
        }

        ScrollPane scrollPane = new ScrollPane(contenedor);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        panel.getChildren().clear();
        panel.getChildren().add(scrollPane);
    }

    // ==================== MÉTODOS UTILITARIOS ====================

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess, Consumer<Throwable> onError, String contexto) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return tarea.get();
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            LOGGER.log(Level.SEVERE, "Error en " + contexto, task.getException());
            onError.accept(task.getException());
        });

        executor.execute(task);
    }

    private void notificarListeners(String tipo, JsonArray datos) {
        listeners.forEach(listener -> {
            if ("contenidos".equals(tipo)) {
                listener.onContenidosActualizados(datos);
            } else if ("solicitudes".equals(tipo)) {
                listener.onSolicitudesActualizadas(datos);
            }
        });
    }

    private Node crearMensajeVacio(String mensaje) {
        Label label = new Label(mensaje);
        label.getStyleClass().add("mensaje-vacio");
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private String formatFecha(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(millis));
    }

    private void manejarError(String contexto, Throwable e) {
        Platform.runLater(() -> {
            mostrarAlerta("Error", "Error en " + contexto + ": " + e.getMessage(), Alert.AlertType.ERROR);
            LOGGER.log(Level.SEVERE, "Error en " + contexto, e);
        });
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ==================== MÉTODOS DE NAVEGACIÓN ====================

    private void abrirAjustes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/ajustes-usuario.fxml"));
            Parent root = loader.load();

            ControladorAjustesUsuario controlador = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Ajustes de Usuario");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            controlador.inicializar(String.valueOf(usuarioData), cliente, stage);
            stage.showAndWait();
        } catch (IOException e) {
            manejarError("abrir ajustes de usuario", e);
        }
    }

    private void abrirPerfil() {
        if (usuarioData == null || !usuarioData.has("id")) {
            mostrarAlerta("Error", "Datos del usuario no disponibles", Alert.AlertType.ERROR);
            return;
        }

        ejecutarTareaAsync(
                () -> {
                    try {
                        // 1. Preparar solicitud
                        JsonObject solicitud = new JsonObject();
                        String tipoSolicitud = usuarioData.has("esModerador") && usuarioData.get("esModerador").getAsBoolean()
                                ? "OBTENER_DATOS_MODERADOR"
                                : "OBTENER_DATOS_PERFIL";
                        solicitud.addProperty("tipo", tipoSolicitud);

                        JsonObject datos = new JsonObject();
                        datos.addProperty("userId", usuarioData.get("id").getAsString());
                        solicitud.add("datos", datos);

                        // 2. Enviar solicitud
                        cliente.getSalida().println(solicitud.toString());
                        cliente.getSalida().flush();

                        // 3. Recibir respuesta
                        String respuesta = cliente.getEntrada().readLine();

                        if (respuesta == null || respuesta.isEmpty()) {
                            throw new IOException("El servidor no respondió");
                        }

                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

                        // 4. Validar respuesta
                        if (!jsonRespuesta.has("exito") || !jsonRespuesta.get("exito").getAsBoolean()) {
                            String mensajeError = jsonRespuesta.has("mensaje")
                                    ? jsonRespuesta.get("mensaje").getAsString()
                                    : "Error desconocido";
                            throw new RuntimeException(mensajeError);
                        }

                        // 5. Preparar datos para retorno
                        JsonObject datosRetorno = new JsonObject();
                        datosRetorno.add("datosUsuario", jsonRespuesta.get("datosUsuario"));
                        datosRetorno.addProperty("esModerador", tipoSolicitud.equals("OBTENER_DATOS_MODERADOR"));

                        return datosRetorno;
                    } catch (Exception e) {
                        throw new RuntimeException("Error al obtener datos del perfil: " + e.getMessage(), e);
                    }
                },
                datosRetorno -> {
                    Platform.runLater(() -> {
                        try {
                            JsonObject datosUsuario = datosRetorno.getAsJsonObject("datosUsuario");
                            boolean esModerador = datosRetorno.get("esModerador").getAsBoolean();

                            String fxmlPath = esModerador
                                    ? "/com/taller/estudiantevistas/fxml/moderador.fxml"
                                    : "/com/taller/estudiantevistas/fxml/perfil.fxml";

                            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                            Parent root = loader.load();

                            // Usamos una interfaz común o hacemos casting según el tipo
                            if (esModerador) {
                                ControladorModerador controlador = loader.getController();
                                controlador.inicializar(datosUsuario, cliente);
                            } else {
                                ControladorPerfil controlador = loader.getController();
                                controlador.inicializar(datosUsuario, cliente);
                            }

                            Stage stage = new Stage();
                            stage.setTitle(esModerador ? "Panel de Moderador" : "Perfil de Usuario");
                            stage.setScene(new Scene(root));
                            stage.initModality(Modality.APPLICATION_MODAL);
                            stage.show();
                        } catch (IOException e) {
                            manejarError("cargar vista de perfil/moderador", e);
                        }
                    });
                },
                error -> {
                    Platform.runLater(() -> {
                        mostrarAlerta("Error", "No se pudo cargar el perfil: " + error.getMessage(),
                                Alert.AlertType.ERROR);
                    });
                    LOGGER.log(Level.SEVERE, "Error al cargar perfil", error);
                },
                "carga de perfil"
        );
    }

    private void mostrarVistaSolicitudAyuda() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/crear-solicitud.fxml"));
            Parent root = loader.load();

            ControladorSolicitudAyuda controlador = loader.getController();
            controlador.inicializar(usuarioData, cliente);

            Stage stage = new Stage();
            stage.setTitle("Solicitud de Ayuda");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            manejarError("mostrar vista de solicitud", e);
        }
    }

    private void mostrarNotificaciones() {
        // Implementación futura
    }

    private void abrirChat() {
        // Implementación futura
    }

    private void abrirContacto() {
        // Implementación futura
    }

    private void cargarContenidosIniciales() {
        recargarContenidos();
        recargarSolicitudes();
    }

    public void addActualizacionListener(ActualizacionListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }




}