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
    // Logger para manejo de errores
    private static final Logger LOGGER = Logger.getLogger(ControladorPrincipal.class.getName());

    // Executor para operaciones asíncronas
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Campos para datos del usuario
    private JsonObject usuarioData;
    private ClienteServicio cliente;

    // Componentes de la barra superior
    @FXML private TextField campoBusqueda;
    @FXML private ComboBox<String> comboTipo;
    @FXML private Button btnBuscar, btnAjustes, btnNotificaciones, btnChat, btnPerfil, btnContacto;

    // ImageViews
    @FXML private ImageView imgLupa, imgAjustes, imgCampana, imgChat, imgPerfil, imgContacto, imgRecargar;

    // Componentes del área central
    @FXML private Button btnRecargarContenidos, btnRecargarSolicitudes, btnNuevaSolicitud;
    @FXML private Pane panelContenidos, panelSolicitudes;

    // Interfaz para listeners de actualización
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

        try {
            Platform.runLater(this::cargarContenidosIniciales);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar con usuario", e);
            mostrarAlerta("Error", "No se pudieron cargar los datos del usuario", Alert.AlertType.ERROR);
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
            imgLupa.setImage(loadImage("/com/taller/estudiantevistas/icons/lupa.png"));
            imgAjustes.setImage(loadImage("/com/taller/estudiantevistas/icons/ajustes.png"));
            imgCampana.setImage(loadImage("/com/taller/estudiantevistas/icons/campana.png"));
            imgChat.setImage(loadImage("/com/taller/estudiantevistas/icons/chat.png"));
            imgPerfil.setImage(loadImage("/com/taller/estudiantevistas/icons/perfil.png"));
            imgContacto.setImage(loadImage("/com/taller/estudiantevistas/icons/contacto.png"));
            imgRecargar.setImage(loadImage("/com/taller/estudiantevistas/icons/recargar.png")); // Nueva línea
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

    // Métodos de acción principales
    private void buscarContenido() {
        String busqueda = campoBusqueda.getText().trim();
        String tipo = comboTipo.getValue();

        if (busqueda.isEmpty()) {
            mostrarAlerta("Búsqueda vacía", "Por favor ingrese un término de búsqueda", Alert.AlertType.WARNING);
            return;
        }

        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "BUSCAR");
                    solicitud.addProperty("userId", usuarioData.get("id").getAsString());
                    solicitud.addProperty("criterio", tipo);
                    solicitud.addProperty("busqueda", busqueda);

                    cliente.getSalida().println(solicitud.toString());
                    String respuesta = null;
                    try {
                        respuesta = cliente.getEntrada().readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return JsonParser.parseString(respuesta).getAsJsonObject().getAsJsonArray("resultados");
                },
                resultados -> {
                    mostrarContenidosEnPanel(resultados, panelContenidos);
                    notificarListeners("contenidos", resultados);
                },
                "búsqueda de contenidos"
        );
    }

    @FXML
    private void recargarContenidos() {
        if (usuarioData != null && usuarioData.has("id") && !usuarioData.get("id").isJsonNull()) {
            ejecutarTareaAsync(
                    () -> cliente.obtenerContenidosEducativos(usuarioData.get("id").getAsString()),
                    contenidos -> {
                        mostrarContenidosEnPanel(contenidos, panelContenidos);
                        notificarListeners("contenidos", contenidos);
                    },
                    "carga de contenidos"
            );
        } else {
            LOGGER.warning("No se puede recargar contenidos: ID de usuario ausente o nulo.");
            mostrarAlerta("Advertencia", "No se pudo obtener el ID del usuario para cargar contenidos.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void recargarSolicitudes() {
        if (usuarioData != null && usuarioData.has("id") && !usuarioData.get("id").isJsonNull()) {
            ejecutarTareaAsync(
                    () -> cliente.obtenerSolicitudesAyuda(usuarioData.get("id").getAsString()),
                    solicitudes -> {
                        mostrarSolicitudesEnPanel(solicitudes, panelSolicitudes);
                        notificarListeners("solicitudes", solicitudes);
                    },
                    "carga de solicitudes"
            );
        } else {
            LOGGER.warning("No se puede recargar solicitudes: ID de usuario ausente o nulo.");
            mostrarAlerta("Advertencia", "No se pudo obtener el ID del usuario para cargar solicitudes.", Alert.AlertType.WARNING);
        }
    }


    private void mostrarDialogoNuevaSolicitud() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Nueva Solicitud de Ayuda");
        dialog.setHeaderText("Complete los detalles de su solicitud");

        // Configurar botones
        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);

        // Crear campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField temaField = new TextField();
        temaField.setPromptText("Tema de la solicitud");

        TextArea descripcionArea = new TextArea();
        descripcionArea.setPromptText("Descripción detallada");
        descripcionArea.setPrefRowCount(3);

        ComboBox<String> urgenciaCombo = new ComboBox<>();
        urgenciaCombo.getItems().addAll("ALTA", "MEDIA", "BAJA");
        urgenciaCombo.setValue("MEDIA");

        grid.add(new Label("Tema:"), 0, 0);
        grid.add(temaField, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(descripcionArea, 1, 1);
        grid.add(new Label("Urgencia:"), 0, 2);
        grid.add(urgenciaCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == crearButtonType) {
                return new Pair<>(temaField.getText(), descripcionArea.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(temaDescripcion -> {
            JsonObject nuevaSolicitud = new JsonObject();
            nuevaSolicitud.addProperty("tema", temaDescripcion.getKey());
            nuevaSolicitud.addProperty("descripcion", temaDescripcion.getValue());
            nuevaSolicitud.addProperty("urgencia", urgenciaCombo.getValue());
            nuevaSolicitud.addProperty("solicitanteId", usuarioData.get("id").getAsString());

            enviarNuevaSolicitud(nuevaSolicitud);
        });
    }

    // Agrega este metodo al ControladorPrincipal
    private void mostrarVistaSolicitudAyuda() {
        try {
            // Cargar la vista FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/crear-solicitud.fxml"));
            Parent root = loader.load();

            // Obtener el controlador y configurarlo
            ControladorSolicitudAyuda controlador = loader.getController();
            controlador.inicializar(usuarioData.get("id").getAsString(), cliente);

            // Crear una nueva escena
            Stage stage = new Stage();
            stage.setTitle("Solicitud de Ayuda");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            manejarError("cargar vista de solicitud de ayuda", e);
        }
    }

    // Modifica el método configurarEventos para usar la nueva vista
    private void configurarEventos() {
        btnBuscar.setOnAction(event -> buscarContenido());
        btnRecargarContenidos.setOnAction(event -> recargarContenidos());
        btnRecargarSolicitudes.setOnAction(event -> recargarSolicitudes());
        btnNuevaSolicitud.setOnAction(event -> mostrarVistaSolicitudAyuda()); // Cambiado para usar la nueva vista
        btnAjustes.setOnAction(event -> abrirAjustes());
        btnNotificaciones.setOnAction(event -> mostrarNotificaciones());
        btnChat.setOnAction(event -> abrirChat());
        btnPerfil.setOnAction(event -> abrirPerfil());
        btnContacto.setOnAction(event -> abrirContacto());
    }

    private void enviarNuevaSolicitud(JsonObject solicitud) {
        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitudCompleta = new JsonObject();
                    solicitudCompleta.addProperty("tipo", "NUEVA_SOLICITUD");
                    solicitudCompleta.add("datos", solicitud);

                    cliente.getSalida().println(solicitudCompleta.toString());
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
                        recargarSolicitudes();
                    } else {
                        mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR);
                    }
                },
                "envío de nueva solicitud"
        );
    }

    // Métodos de visualización
    private void mostrarContenidosEnPanel(JsonArray contenidos, Pane panel) {
        mostrarContenidoGenerico(
                contenidos,
                panel,
                contenido -> crearItemContenido(
                        contenido.get("titulo").getAsString(),
                        contenido.get("autor").getAsString(),
                        contenido.get("tema").getAsString()
                )
        );
    }

    private void mostrarSolicitudesEnPanel(JsonArray solicitudes, Pane panel) {
        mostrarContenidoGenerico(
                solicitudes,
                panel,
                solicitud -> crearItemSolicitud(
                        solicitud.get("tema").getAsString(),
                        solicitud.get("descripcion").getAsString(),
                        solicitud.get("urgencia").getAsString(),
                        solicitud.get("estado").getAsString(),
                        solicitud.get("fecha").getAsLong(),
                        solicitud.has("solicitanteNombre") ?
                                solicitud.get("solicitanteNombre").getAsString() :
                                "ID: " + solicitud.get("solicitanteId").getAsString().substring(0, 6)
                )
        );
    }

    private void mostrarContenidoGenerico(JsonArray datos, Pane panel, Function<JsonObject, Node> factory) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(10));

        if (datos.size() == 0) {
            contenedor.getChildren().add(crearMensajeVacio("No hay datos disponibles"));
        } else {
            datos.forEach(item ->
                    contenedor.getChildren().add(factory.apply(item.getAsJsonObject()))
            );
        }

        panel.getChildren().clear();
        panel.getChildren().add(crearPanelScrollable(contenedor));
    }

    private Node crearItemContenido(String titulo, String autor, String tema) {
        VBox item = new VBox(5);
        item.getStyleClass().add("contenido-item");

        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("contenido-titulo");

        Label autorLabel = new Label("Autor: " + autor);
        autorLabel.getStyleClass().add("contenido-info");

        Label temaLabel = new Label("Tema: " + tema);
        temaLabel.getStyleClass().add("contenido-info");

        item.getChildren().addAll(tituloLabel, autorLabel, temaLabel);
        return item;
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

    private Node crearMensajeVacio(String mensaje) {
        Label label = new Label(mensaje);
        label.getStyleClass().add("mensaje-vacio");
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private ScrollPane crearPanelScrollable(Node contenido) {
        ScrollPane scrollPane = new ScrollPane(contenido);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    // Métodos utilitarios
    private String formatFecha(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(millis));
    }

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess, String contexto) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return tarea.get();
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> manejarError(contexto, task.getException()));

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

    public void addActualizacionListener(ActualizacionListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    private void manejarError(String contexto, Throwable e) {
        Platform.runLater(() ->
                mostrarAlerta("Error", "Error en " + contexto + ": " + e.getMessage(), Alert.AlertType.ERROR)
        );
        LOGGER.log(Level.SEVERE, "Error en " + contexto, e);
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Métodos de navegación (sin cambios)
    private void abrirAjustes() {
        try {
            // Ruta correcta del FXML
            URL fxmlLocation = getClass().getResource("/com/taller/estudiantevistas/fxml/ajustes-usuario.fxml");
            if (fxmlLocation == null) {
                throw new FileNotFoundException("No se encontró el archivo FXML en la ruta: /com/taller/estudiantevistas/fxml/ajustes-usuario.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            // Obtener el controlador y pasarle los datos necesarios
            ControladorAjustesUsuario controlador = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Ajustes de Usuario");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Ventana bloqueante

            controlador.inicializar(String.valueOf(usuarioData), cliente, stage);
            stage.showAndWait();

        } catch (IOException e) {
            manejarError("Error de entrada/salida al abrir ajustes de usuario", e);
        } catch (Exception e) {
            manejarError("Error inesperado al abrir ajustes de usuario", e);
        }
    }




    private void mostrarNotificaciones() {
        System.out.println("Mostrando notificaciones para: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
    }

    private void abrirChat() {
        System.out.println("Abriendo chat para: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
    }

    private void abrirPerfil() {
        if (usuarioData != null) {
            System.out.println("Abriendo perfil de: " + usuarioData.get("nombre").getAsString());
        }
    }

    private void abrirContacto() {
        System.out.println("Abriendo contacto para: " +
                (usuarioData != null ? usuarioData.get("nombre").getAsString() : "desconocido"));
    }

    private void cargarContenidosIniciales() {
        recargarContenidos();
        recargarSolicitudes();
    }
}