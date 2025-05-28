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
import javafx.scene.text.Text;

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

/**
 * Controlador para la vista de contenidos del perfil de usuario.
 * Permite cargar y mostrar los contenidos publicados por un usuario específico.
 */

public class ControladorContenidosPerfil {
    private static final Logger LOGGER = Logger.getLogger(ControladorContenidosPerfil.class.getName());

    // Executor para manejar tareas asíncronas
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });


    @FXML private Pane panelContenidos;
    private String userId;
    private ClienteServicio cliente;
    private BiConsumer<String, Consumer<JsonArray>> cargadorContenidos;

    /**
     * Inicializa el controlador con el ID del usuario, el cliente de servicio y un cargador de contenidos.
     * @param userId ID del usuario cuyos contenidos se van a cargar.
     * @param cliente ClienteServicio para la comunicación con el servidor.
     * @param cargadorContenidos Función que carga los contenidos del usuario de forma asíncrona.
     */

    public void inicializar(String userId, ClienteServicio cliente, BiConsumer<String, Consumer<JsonArray>> cargadorContenidos) {
        this.userId = userId;
        this.cliente = cliente;
        this.cargadorContenidos = cargadorContenidos;
        cargarContenidosUsuario();
    }

    /**
     * Carga los contenidos del usuario especificado.
     * Si se proporciona un cargador de contenidos, lo utiliza para cargar los datos.
     * Si no, utiliza la implementación original para obtener los contenidos del servidor.
     */
    private void cargarContenidosUsuario() {
        panelContenidos.getChildren().clear();

        if (cargadorContenidos != null) {
            cargadorContenidos.accept(userId, contenidos -> {
                Platform.runLater(() -> {
                    if (contenidos == null || contenidos.size() == 0) {
                        mostrarContenidosEnPanel(crearMensajeInformacion(
                                "No hay contenidos",
                                "No has publicado ningún contenido aún"));
                    } else {
                        mostrarContenidosEnPanel(contenidos);
                    }
                });
            });
        } else {
            ejecutarTareaAsync(
                    () -> {
                        try {
                            JsonObject solicitud = new JsonObject();
                            JsonObject datos = new JsonObject();
                            datos.addProperty("userId", userId);

                            solicitud.addProperty("tipo", "OBTENER_CONTENIDOS_USUARIO");
                            solicitud.add("datos", datos);

                            cliente.getSalida().println(solicitud.toString());
                            String respuesta = cliente.getEntrada().readLine();
                            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

                            if (!jsonRespuesta.get("exito").getAsBoolean()) {
                                throw new RuntimeException(jsonRespuesta.get("mensaje").getAsString());
                            }

                            JsonArray contenidos = jsonRespuesta.getAsJsonArray("contenidos");
                            if (contenidos == null || contenidos.size() == 0) {
                                return crearMensajeInformacion(
                                        "No hay contenidos",
                                        "No has publicado ningún contenido aún");
                            }
                            return filtrarDuplicados(contenidos);
                        } catch (IOException e) {
                            throw new RuntimeException("Error al obtener contenidos: " + e.getMessage(), e);
                        }
                    },
                    contenidos -> Platform.runLater(() -> mostrarContenidosEnPanel(contenidos)),
                    error -> Platform.runLater(() -> {
                        mostrarContenidosEnPanel(crearMensajeError(error));
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR);
                    }),
                    "carga de contenidos del usuario"
            );
        }
    }

    /**
     * Filtra los contenidos duplicados basándose en el campo "id".
     * Si un contenido no tiene "id", se considera único.
     * @param array JsonArray de contenidos a filtrar.
     * @return JsonArray sin duplicados.
     */

    private JsonArray filtrarDuplicados(JsonArray array) {
        Set<String> idsVistos = new HashSet<>();
        JsonArray resultado = new JsonArray();

        for (JsonElement elemento : array) {
            JsonObject obj = elemento.getAsJsonObject();
            String id = obj.has("id") ? obj.get("id").getAsString() : null;

            if (id == null || !idsVistos.contains(id)) {
                resultado.add(obj);
                if (id != null) {
                    idsVistos.add(id);
                }
            }
        }
        return resultado;
    }

    /**
     * Muestra los contenidos en el panel de contenidos.
     * Si no hay contenidos, muestra un mensaje informativo.
     * Si hay un mensaje especial, lo muestra en lugar de los contenidos.
     * @param contenidos JsonArray de contenidos a mostrar.
     */

    private void mostrarContenidosEnPanel(JsonArray contenidos) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(10));
        contenedor.setStyle("-fx-background-color: #f5f5f5;");
        contenedor.setMaxWidth(panelContenidos.getWidth() - 20);

        if (contenidos == null || contenidos.size() == 0) {
            contenedor.getChildren().add(crearMensajeUI(
                    "No hay contenidos",
                    "No has publicado ningún contenido aún",
                    this::cargarContenidosUsuario
            ));
        } else if (esMensajeEspecial(contenidos)) {
            JsonObject mensaje = contenidos.get(0).getAsJsonObject();
            contenedor.getChildren().add(crearMensajeUI(
                    mensaje.get("titulo").getAsString(),
                    mensaje.get("detalle").getAsString(),
                    this::cargarContenidosUsuario
            ));
        } else {
            Set<String> idsMostrados = new HashSet<>();
            for (JsonElement elemento : contenidos) {
                JsonObject contenido = elemento.getAsJsonObject();
                String id = contenido.has("id") ? contenido.get("id").getAsString() : null;

                if (id == null || !idsMostrados.contains(id)) {
                    Node item = crearItemContenido(contenido);
                    if (item != null) {
                        contenedor.getChildren().add(item);
                        if (id != null) {
                            idsMostrados.add(id);
                        }
                    }
                }
            }

            if (contenedor.getChildren().isEmpty()) {
                contenedor.getChildren().add(crearMensajeUI(
                        "No hay contenidos visibles",
                        "Todos los contenidos están duplicados o no son válidos",
                        this::cargarContenidosUsuario
                ));
            }
        }

        ScrollPane scrollPane = crearScrollPane(contenedor);
        scrollPane.setPrefViewportWidth(panelContenidos.getWidth() - 15);
        panelContenidos.getChildren().clear();
        panelContenidos.getChildren().add(scrollPane);
    }

    /**
     * Crea un nodo que representa un contenido individual.
     * Incluye cabecera con icono de tipo, título, metadatos y descripción.
     * @param contenido JsonObject que representa el contenido a mostrar.
     * @return Node que representa el contenido visualmente.
     */

    private Node crearItemContenido(JsonObject contenido) {
        VBox item = new VBox(10);
        item.getStyleClass().add("contenido-item");

        // Cabecera con icono de tipo y título
        HBox header = new HBox(10);
        header.getStyleClass().add("contenido-header");
        header.setAlignment(Pos.CENTER_LEFT);

        String tipo = contenido.has("tipo") ? contenido.get("tipo").getAsString() : "DESCONOCIDO";
        Label tipoIcon = new Label(obtenerIconoTipo(tipo));
        tipoIcon.getStyleClass().addAll("tipo-icono", "tipo-" + tipo.toLowerCase());

        Label titulo = new Label(contenido.has("titulo") ? contenido.get("titulo").getAsString() : "Sin título");
        titulo.getStyleClass().add("contenido-titulo");

        header.getChildren().addAll(tipoIcon, titulo);

        // Metadatos
        HBox metadatos = new HBox(15);
        metadatos.getStyleClass().add("metadata-row");

        Label autor = new Label("👤 " + (contenido.has("autor") ? contenido.get("autor").getAsString() : "Anónimo"));
        autor.getStyleClass().add("metadata-label");

        Label fecha = new Label("📅 " + (contenido.has("fechaCreacion") ?
                formatFechaContenido(contenido.get("fechaCreacion").getAsString()) : "Fecha desconocida"));
        fecha.getStyleClass().add("metadata-label");

        Label tema = new Label("🏷 " + (contenido.has("tema") ? contenido.get("tema").getAsString() : "Sin tema"));
        tema.getStyleClass().add("metadata-label");

        metadatos.getChildren().addAll(autor, fecha, tema);

        // Descripción
        Text descripcion = new Text(contenido.has("descripcion") ?
                contenido.get("descripcion").getAsString() : "Sin descripción disponible");
        descripcion.getStyleClass().add("descripcion-text");
        descripcion.setWrappingWidth(panelContenidos.getWidth() - 40);

        // Contenido principal
        Node contenidoVisual = crearVisualizacionContenido(contenido);

        item.getChildren().addAll(header, metadatos, descripcion, contenidoVisual);
        return item;
    }

    /**
     * Crea una visualización del contenido dependiendo de su tipo.
     * Si es un enlace, crea un Hyperlink; si es texto, un TextArea.
     * @param contenido JsonObject que contiene los datos del contenido.
     * @return Node que representa la visualización del contenido.
     */

    private Node crearVisualizacionContenido(JsonObject contenido) {
        String tipo = contenido.has("tipo") ? contenido.get("tipo").getAsString() : "TEXTO";
        String contenidoStr = contenido.has("contenido") ? contenido.get("contenido").getAsString() : "Contenido no disponible";

        if (tipo.equals("ENLACE")) {
            HBox linkContainer = new HBox();
            linkContainer.setAlignment(Pos.CENTER_LEFT);

            Hyperlink link = new Hyperlink(contenidoStr);
            link.setStyle("-fx-text-fill: #0066cc; -fx-underline: true;");
            link.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(contenidoStr));
                } catch (Exception ex) {
                    mostrarAlerta("Error", "No se pudo abrir el enlace: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });

            Label icono = new Label("🌐");
            linkContainer.getChildren().addAll(icono, link);
            return linkContainer;
        } else {
            TextArea areaTexto = new TextArea(contenidoStr);
            areaTexto.setEditable(false);
            areaTexto.setWrapText(true);
            areaTexto.setPrefRowCount(4);
            areaTexto.setFocusTraversable(false);
            areaTexto.setStyle("-fx-background-color: #f9f9f9;");
            return areaTexto;
        }
    }

    /**
     * Crea un ScrollPane para el contenido dado.
     * Configura el ScrollPane para que se ajuste al ancho del panel y tenga un estilo personalizado.
     * @param contenido Nodo que se mostrará dentro del ScrollPane.
     * @return ScrollPane configurado con el contenido.
     */

    private ScrollPane crearScrollPane(Node contenido) {
        ScrollPane scrollPane = new ScrollPane(contenido);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPadding(new Insets(0));
        scrollPane.setPrefViewportWidth(panelContenidos.getWidth() - 15);
        scrollPane.getStyleClass().add("scroll-pane");
        return scrollPane;
    }

    /**
     * Formatea una fecha en el formato "dd/MM/yyyy HH:mm".
     * Si la fecha no es válida, devuelve la cadena original.
     * @param fechaStr Fecha en formato "yyyy-MM-dd HH:mm:ss".
     * @return Fecha formateada o la cadena original si hay un error.
     */

    private String formatFechaContenido(String fechaStr) {
        try {
            SimpleDateFormat formatoOriginal = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date fecha = formatoOriginal.parse(fechaStr);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fecha);
        } catch (Exception e) {
            return fechaStr;
        }
    }

    /**
     * Obtiene un icono representativo del tipo de contenido.
     * Utiliza emojis para representar diferentes tipos de contenido.
     * @param tipo Tipo de contenido (ej. "VIDEO", "DOCUMENTO", etc.).
     * @return String con el emoji correspondiente al tipo de contenido.
     */

    private String obtenerIconoTipo(String tipo) {
        switch(tipo.toUpperCase()) {
            case "VIDEO": return "🎬";
            case "DOCUMENTO": return "📄";
            case "ENLACE": return "🔗";
            case "IMAGEN": return "🖼";
            case "AUDIO": return "🎧";
            default: return "📌";
        }
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