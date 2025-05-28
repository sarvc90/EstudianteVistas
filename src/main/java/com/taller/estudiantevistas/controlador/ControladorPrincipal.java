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

import java.io.IOException;
import java.text.ParseException;
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

    // Configuración de ejecución asíncrona
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Datos del usuario y cliente de servicio
    private JsonObject usuarioData;
    private ClienteServicio cliente;

    // Componentes de la UI
    @FXML private TextField campoBusqueda;
    @FXML private ComboBox<String> comboTipo;
    @FXML private Button btnBuscar, btnAjustes, btnChat, btnPerfil, btnContacto;
    @FXML private ImageView imgLupa, imgAjustes, imgChat, imgPerfil, imgContacto, imgRecargar;
    @FXML private Button btnRecargarContenidos, btnRecargarSolicitudes, btnNuevaSolicitud;
    @FXML private Pane panelContenidos, panelSolicitudes;

    private static final String[] TIPOS_CONTENIDO = {"Tema", "Autor", "Fecha"};
    private static final String[] FILTROS_SOLICITUD = {"Todas", "Pendientes", "En proceso", "Resueltas"};

    // Listeners para actualizaciones
    private final List<ActualizacionListener> listeners = new ArrayList<>();

    // Interfaz para notificaciones de actualización
    public interface ActualizacionListener {
        void onContenidosActualizados(JsonArray contenidos);
        void onSolicitudesActualizadas(JsonArray solicitudes);
    }

    /**
     * Inicializa el controlador con los datos del usuario y el cliente de servicio
     */
    public void inicializarConUsuario(JsonObject usuarioData, ClienteServicio cliente) {
        Objects.requireNonNull(usuarioData, "Datos de usuario no pueden ser nulos");
        Objects.requireNonNull(cliente, "ClienteServicio no puede ser nulo");

        this.usuarioData = usuarioData;
        this.cliente = cliente;

        // Cargar datos iniciales en el hilo de la UI
        Platform.runLater(() -> {
            cargarContenidosIniciales();
            configurarEventos();
            cargarImagenes();
            configurarComboBox();
            if (esModerador()) {
                btnContacto.setVisible(false);
                imgContacto.setVisible(false);
                btnNuevaSolicitud.setVisible(false);
            }
        });
        panelContenidos.getStyleClass().add("panel-contenedor");
        panelSolicitudes.getStyleClass().add("panel-contenedor");
    }
    private boolean esModerador() {
        if (usuarioData == null) return false;

        // Primera forma de verificación: campo "esModerador"
        if (usuarioData.has("esModerador")) {
            return usuarioData.get("esModerador").getAsBoolean();
        }

        // Segunda forma de verificación: campo "rol" con valor "MODERADOR"
        if (usuarioData.has("rol")) {
            return "MODERADOR".equalsIgnoreCase(usuarioData.get("rol").getAsString());
        }

        return false;
    }

    // ==================== CONFIGURACIÓN INICIAL ====================

    private void cargarImagenes() {
        try {
            imgLupa.setImage(cargarImagen("/com/taller/estudiantevistas/icons/lupa.png"));
            imgAjustes.setImage(cargarImagen("/com/taller/estudiantevistas/icons/ajustes.png"));
            imgChat.setImage(cargarImagen("/com/taller/estudiantevistas/icons/chat.png"));
            imgPerfil.setImage(cargarImagen("/com/taller/estudiantevistas/icons/perfil.png"));
            imgContacto.setImage(cargarImagen("/com/taller/estudiantevistas/icons/contacto.png"));
            imgRecargar.setImage(cargarImagen("/com/taller/estudiantevistas/icons/recargar.png"));
        } catch (Exception e) {
            manejarError("cargar imágenes", e);
        }
    }

    private Image cargarImagen(String path) throws IOException {
        return new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(path),
                "No se pudo cargar imagen: " + path
        ));
    }

    private void configurarComboBox() {
        comboTipo.getItems().addAll(TIPOS_CONTENIDO);
        comboTipo.getSelectionModel().selectFirst();

        // Estilo adicional para el combo box
        comboTipo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                if (!empty && getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: #2a2a3a; -fx-text-fill: white;");
                } else if (!empty) {
                    setStyle("-fx-background-color: #252535; -fx-text-fill: white;");
                }
            }
        });
    }

    private void configurarEventos() {
        // Limpiar eventos primero para evitar duplicados
        btnBuscar.setOnAction(null);
        btnRecargarContenidos.setOnAction(null);
        btnRecargarSolicitudes.setOnAction(null);
        btnNuevaSolicitud.setOnAction(null);
        btnAjustes.setOnAction(null);
        btnChat.setOnAction(null);
        btnPerfil.setOnAction(null);
        btnContacto.setOnAction(null);

        // Ahora asignar los eventos
        btnBuscar.setOnAction(event -> buscarContenido());
        btnRecargarContenidos.setOnAction(event -> recargarContenidos());
        btnRecargarSolicitudes.setOnAction(event -> recargarSolicitudes());
        btnNuevaSolicitud.setOnAction(event -> mostrarVistaSolicitudAyuda());
        btnAjustes.setOnAction(event -> abrirAjustes());
        btnChat.setOnAction(event -> abrirChat());
        btnPerfil.setOnAction(event -> abrirPerfil());
        btnContacto.setOnAction(event -> abrirContacto());
    }

    // ==================== MÉTODOS DE CARGA DE DATOS ====================

    /**
     * Carga los contenidos iniciales al iniciar la vista
     */
    private void cargarContenidosIniciales() {
        recargarContenidos();
        recargarSolicitudes();
    }

    // ==================== MÉTODOS DE CARGA IDÉNTICOS A SOLICITUDES ====================

    /**
     * Recarga los contenidos educativos - Mismo enfoque que recargarSolicitudes()
     */
    @FXML
    private void recargarContenidos() {
        if (usuarioData != null && usuarioData.has("id")) {
            // Limpiar panel antes de cargar
            panelContenidos.getChildren().clear();

            ejecutarTareaAsync(
                    () -> {
                        try {
                            JsonArray contenidos = cliente.obtenerTodosContenidos();
                            if (contenidos == null || contenidos.size() == 0) {
                                return crearMensajeInformacion(
                                        "No hay contenidos",
                                        "No se encontraron contenidos educativos disponibles");
                            }
                            // Verificar duplicados en el servidor
                            return filtrarDuplicados(contenidos);
                        } catch (IOException e) {
                            throw new RuntimeException("Error al obtener contenidos: " + e.getMessage(), e);
                        }
                    },
                    contenidos -> Platform.runLater(() -> {
                        mostrarContenidosEnPanel(contenidos, panelContenidos);
                        notificarListeners("contenidos", contenidos);
                    }),
                    error -> Platform.runLater(() -> {
                        mostrarContenidosEnPanel(crearMensajeError(error), panelContenidos);
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR);
                    }),
                    "recarga de contenidos"
            );
        }
    }

    // Método para filtrar duplicados
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
     * Muestra contenidos en el panel - IDÉNTICO a mostrarSolicitudesEnPanel()
     */
    /**
    private void mostrarContenidosEnPanel(JsonArray contenidos, Pane panel) {
        VBox contenedor = new VBox(5);
        contenedor.setPadding(new Insets(10));
        contenedor.setStyle("-fx-background-color: #f5f5f5;");

        // Limpiar el contenedor primero
        contenedor.getChildren().clear();

        if (esMensajeEspecial(contenidos)) {
            JsonObject mensaje = contenidos.get(0).getAsJsonObject();
            contenedor.getChildren().add(crearMensajeUI(
                    mensaje.get("titulo").getAsString(),
                    mensaje.get("detalle").getAsString(),
                    this::recargarContenidos
            ));
        } else {
            Set<String> idsMostrados = new HashSet<>(); // Para evitar duplicados en la UI
            for (JsonElement elemento : contenidos) {
                JsonObject contenido = elemento.getAsJsonObject();
                String id = contenido.has("id") ? contenido.get("id").getAsString() : null;

                if (id == null || !idsMostrados.contains(id)) {
                    contenedor.getChildren().add(crearItemContenidoEstiloSolicitud(contenido));
                    if (id != null) {
                        idsMostrados.add(id);
                    }
                }
            }
        }

        panel.getChildren().clear();
        panel.getChildren().add(crearScrollPane(contenedor));
    }*/

    /**
     * Crea items de contenido con el MISMO ESTILO que las solicitudes
     */
    private Node crearItemContenidoEstiloSolicitud(JsonObject contenido) {
        VBox item = new VBox(5);
        item.getStyleClass().add("contenido-item");
        item.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10;");

        // Título (igual que en solicitudes)
        Label titulo = new Label(contenido.get("titulo").getAsString());
        titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setWrapText(true);

        // Metadatos en línea (como en solicitudes)
        HBox metadatos = new HBox(8);
        metadatos.setAlignment(Pos.CENTER_LEFT);

        Label autor = new Label("👤 " + contenido.get("autor").getAsString());
        Label tipo = new Label("📋 " + contenido.get("tipo").getAsString());

        metadatos.getChildren().addAll(autor, tipo);

        // Descripción (idéntico a solicitudes)
        TextArea descripcion = new TextArea(contenido.get("descripcion").getAsString());
        descripcion.setEditable(false);
        descripcion.setWrapText(true);
        descripcion.setPrefRowCount(3);
        descripcion.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // Contenido (adaptación mínima)
        String contenidoStr = contenido.get("contenido").getAsString();
        TextArea areaContenido = new TextArea(contenidoStr.length() > 100 ? contenidoStr.substring(0, 100) + "..." : contenidoStr);
        areaContenido.setEditable(false);
        areaContenido.setWrapText(true);
        areaContenido.setPrefRowCount(2);
        areaContenido.setStyle("-fx-background-color: #f9f9f9;");

        item.getChildren().addAll(titulo, metadatos, descripcion, areaContenido);
        return item;
    }

    private JsonArray obtenerContenidosDelServidor() throws IOException {
        JsonArray contenidos = cliente.obtenerTodosContenidos();

        if (contenidos == null || contenidos.size() == 0) {
            return crearMensajeInformacion("Sin contenidos", "No se encontraron contenidos educativos");
        }
        return contenidos;
    }

    /**
     * Recarga las solicitudes de ayuda desde el servidor
     */
    @FXML
    private void recargarSolicitudes() {
        if (usuarioData != null && usuarioData.has("id")) {
            // Limpiar panel antes de cargar
            panelSolicitudes.getChildren().clear();

            ejecutarTareaAsync(
                    () -> {
                        try {
                            JsonArray solicitudes = cliente.obtenerTodasSolicitudes();
                            if (solicitudes == null || solicitudes.size() == 0) {
                                return crearMensajeInformacion(
                                        "No hay solicitudes",
                                        "No se encontraron solicitudes pendientes");
                            }
                            // Filtrar posibles duplicados
                            return filtrarDuplicadosSolicitudes(solicitudes);
                        } catch (IOException e) {
                            throw new RuntimeException("Error al obtener solicitudes: " + e.getMessage(), e);
                        }
                    },
                    solicitudes -> Platform.runLater(() -> {
                        mostrarSolicitudesEnPanel(solicitudes, panelSolicitudes);
                        notificarListeners("solicitudes", solicitudes);
                    }),
                    error -> Platform.runLater(() -> {
                        mostrarSolicitudesEnPanel(crearMensajeError(error), panelSolicitudes);
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR);
                    }),
                    "recarga de solicitudes"
            );
        }
    }

    private JsonArray filtrarDuplicadosSolicitudes(JsonArray solicitudes) {
        Set<String> idsUnicos = new HashSet<>();
        JsonArray resultado = new JsonArray();

        for (JsonElement elemento : solicitudes) {
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

    private JsonArray obtenerSolicitudesDelServidor() throws IOException {
        JsonArray solicitudes = cliente.obtenerTodasSolicitudes();

        if (solicitudes == null || solicitudes.size() == 0) {
            return crearMensajeInformacion("No hay solicitudes", "No se encontraron solicitudes pendientes");
        }
        return solicitudes;
    }

    private boolean esFechaValida(String fechaStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            sdf.parse(fechaStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Busca contenidos según los criterios especificados
     */

    private void buscarContenido() {
        String busqueda = campoBusqueda.getText().trim();
        String tipoBusqueda = comboTipo.getValue();

        if (busqueda.isEmpty()) {
            mostrarAlerta("Búsqueda vacía", "Por favor ingrese un término de búsqueda", Alert.AlertType.WARNING);
            return;
        }

        // Validación especial para búsqueda por fecha
        if (tipoBusqueda.equals("Fecha") && !esFechaValida(busqueda)) {
            mostrarAlerta("Formato inválido", "Por favor ingrese una fecha en formato YYYY-MM-DD", Alert.AlertType.WARNING);
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
                    } catch (Exception e) {
                        throw new RuntimeException("Error al buscar contenidos: " + e.getMessage(), e);
                    }
                },
                resultados -> {
                    if (resultados.size() == 0) {
                        Platform.runLater(() ->
                                mostrarAlerta("Sin resultados", "No se encontraron contenidos con esos criterios", Alert.AlertType.INFORMATION)
                        );
                    } else {
                        Platform.runLater(() -> mostrarVistaBusqueda(resultados, tipoBusqueda, busqueda));
                    }
                },
                error -> Platform.runLater(() -> {
                    LOGGER.log(Level.SEVERE, "Error en búsqueda", error);
                    mostrarAlerta("Error", "Ocurrió un error al realizar la búsqueda: " + error.getMessage(), Alert.AlertType.ERROR);
                }),
                "búsqueda de contenidos"
        );
    }

    private void mostrarVistaBusqueda(JsonArray resultados, String tipoBusqueda, String terminoBusqueda) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/buscar.fxml"));
            Parent root = loader.load();

            ControladorBuscar controlador = loader.getController();
            controlador.inicializar(resultados, tipoBusqueda, terminoBusqueda, cliente, usuarioData);

            Stage stage = new Stage();
            stage.setTitle("Resultados de búsqueda");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            manejarError("mostrar vista de búsqueda", e);
        }
    }

    private JsonArray buscarContenidoEnServidor(String tipoBusqueda, String busqueda) throws IOException {
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
    }

    // ==================== MÉTODOS DE VISUALIZACIÓN ====================

    /**
     * Muestra las solicitudes de ayuda en el panel especificado
     */

    private void mostrarSolicitudesEnPanel(JsonArray solicitudes, Pane panel) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(10));
        contenedor.setStyle("-fx-background-color: #f5f5f5;");
        contenedor.setMaxWidth(panel.getWidth() - 20);

        // Limpiar contenedor primero
        contenedor.getChildren().clear();

        if (esMensajeEspecial(solicitudes)) {
            JsonObject mensaje = solicitudes.get(0).getAsJsonObject();
            contenedor.getChildren().add(crearMensajeUI(
                    mensaje.get("titulo").getAsString(),
                    mensaje.get("detalle").getAsString(),
                    this::recargarSolicitudes
            ));
        } else {
            Set<String> idsMostrados = new HashSet<>();
            for (JsonElement elemento : solicitudes) {
                try {
                    JsonObject solicitud = elemento.getAsJsonObject();
                    String id = solicitud.has("id") ? solicitud.get("id").getAsString() :
                            solicitud.has("fecha") ? solicitud.get("fecha").getAsString() : null;

                    if (id == null || !idsMostrados.contains(id)) {
                        contenedor.getChildren().add(crearItemSolicitud(solicitud));
                        if (id != null) {
                            idsMostrados.add(id);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al procesar solicitud", e);
                }
            }
        }
        ScrollPane scrollPane = crearScrollPane(contenedor);
        scrollPane.setPrefViewportWidth(panel.getWidth() - 15);
        panel.getChildren().clear();
        panel.getChildren().add(scrollPane);
    }

    // ==================== COMPONENTES UI ====================

    /**
     * Crea un ítem de contenido educativo para mostrar en la UI (versión mejorada)
     */
    /**
    private Node crearItemContenido(JsonObject contenido) {
        VBox item = new VBox(5); // Espaciado reducido para mejor compactación
        item.getStyleClass().add("contenido-item");
        item.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10;");

        // Título (1 línea)
        Label titulo = new Label(contenido.get("titulo").getAsString());
        titulo.getStyleClass().add("contenido-titulo");
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setWrapText(true);
        titulo.setMaxHeight(20);

        // Metadatos en una línea compacta
        HBox metadatos = new HBox(8);
        metadatos.getStyleClass().add("contenido-metadatos");
        metadatos.setAlignment(Pos.CENTER_LEFT);

        Label autor = new Label("👤 " + contenido.get("autor").getAsString());
        Label fecha = new Label("📅 " + formatFechaContenido(contenido.get("fechaCreacion").getAsString()));
        Label tema = new Label("🏷 " + contenido.get("tema").getAsString());
        Label tipo = new Label("📋 " + contenido.get("tipo").getAsString());

        metadatos.getChildren().addAll(autor, fecha, tema, tipo);

        // Descripción compacta (3-4 líneas máximo)
        TextArea descripcion = new TextArea(contenido.get("descripcion").getAsString());
        descripcion.getStyleClass().add("descripcion-text");
        descripcion.setEditable(false);
        descripcion.setWrapText(true);
        descripcion.setPrefRowCount(3);
        descripcion.setFocusTraversable(false);

        // Contenido principal (dependiendo del tipo)
        Node contenidoVisual = crearVisualizacionContenido(contenido);
        contenidoVisual.getStyleClass().add("contenido-visual");

        item.getChildren().addAll(titulo, metadatos, descripcion, contenidoVisual);
        return item;
    }
*/
    /**
     * Mejora el formato de visualización del contenido según su tipo
     */
    private Node crearVisualizacionContenido(JsonObject contenido) {
        String tipo = contenido.get("tipo").getAsString();
        String contenidoStr = contenido.get("contenido").getAsString();

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
            areaTexto.setPrefRowCount(4); // Limitar altura
            areaTexto.setFocusTraversable(false);
            areaTexto.setStyle("-fx-background-color: #f9f9f9;");
            return areaTexto;
        }
    }

    private String formatFechaContenido(String fechaStr) {
        try {
            SimpleDateFormat formatoOriginal = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date fecha = formatoOriginal.parse(fechaStr);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fecha);
        } catch (Exception e) {
            return fechaStr; // Si falla el parseo, devolver el original
        }
    }

    /**
     * Crea un ítem de solicitud de ayuda para mostrar en la UI
     */

    private Node crearItemSolicitud(JsonObject solicitud) {
        VBox item = new VBox();
        item.getStyleClass().add("solicitud-item");

        // Extraer datos
        String tema = solicitud.has("tema") ? solicitud.get("tema").getAsString() : "Sin tema";
        String descripcion = solicitud.has("descripcion") ? solicitud.get("descripcion").getAsString() : "";
        String urgencia = solicitud.has("urgencia") ? solicitud.get("urgencia").getAsString() : "MEDIA";
        String estado = solicitud.has("estado") ? solicitud.get("estado").getAsString() : "PENDIENTE";

        // Manejo de fecha
        String fechaStr = solicitud.has("fecha") ?
                (solicitud.get("fecha").isJsonPrimitive() && solicitud.get("fecha").getAsJsonPrimitive().isNumber() ?
                        formatFecha(solicitud.get("fecha").getAsLong()) :
                        solicitud.get("fecha").getAsString()) :
                "Fecha no disponible";

        String solicitante = obtenerNombreSolicitante(solicitud);

        // Título (1 línea)
        Label temaLabel = new Label(tema);
        temaLabel.getStyleClass().add("solicitud-titulo");
        temaLabel.setMaxWidth(Double.MAX_VALUE);
        temaLabel.setWrapText(true);
        temaLabel.setMaxHeight(20);

        // Estados y urgencia en una línea
        HBox estadosBox = new HBox(8);
        estadosBox.getStyleClass().add("solicitud-estados");

        Label urgenciaLabel = new Label("🔺 " + urgencia);
        urgenciaLabel.getStyleClass().add("urgencia-" + urgencia.toLowerCase());

        Label estadoLabel = new Label("◉ " + estado);
        estadoLabel.getStyleClass().add("estado-" + estado.toLowerCase());

        estadosBox.getChildren().addAll(urgenciaLabel, estadoLabel);

        // Descripción compacta (3-4 líneas máximo)
        TextArea descripcionArea = new TextArea(descripcion);
        descripcionArea.getStyleClass().add("descripcion-text");
        descripcionArea.setEditable(false);
        descripcionArea.setWrapText(true);
        descripcionArea.setPrefRowCount(3);
        descripcionArea.setFocusTraversable(false);

        // Footer compacto
        HBox footer = new HBox(8);
        footer.getStyleClass().add("solicitud-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        Label fechaLabel = new Label("📅 " + fechaStr);
        Label solicitanteLabel = new Label("👤 " + solicitante);
        footer.getChildren().addAll(fechaLabel, solicitanteLabel);

        item.getChildren().addAll(temaLabel, estadosBox, descripcionArea, footer);
        return item;
    }

    /**
     * Crea un mensaje UI para mostrar cuando no hay datos o hay errores
     */
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
/**
    private ScrollPane crearScrollPane(Node contenido) {
        ScrollPane scrollPane = new ScrollPane(contenido);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }*/

    // ==================== UTILIDADES ====================

    private String obtenerNombreSolicitante(JsonObject solicitud) {
        if (solicitud.has("solicitanteNombre")) {
            return solicitud.get("solicitanteNombre").getAsString();
        } else if (solicitud.has("solicitanteId")) {
            return "ID: " + solicitud.get("solicitanteId").getAsString().substring(0, 6);
        }
        return "Anónimo";
    }

    private String formatFecha(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(millis));
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

            controlador.inicializar(usuarioData.toString(), cliente, stage);
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
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_DATOS_PERFIL");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("userId", usuarioData.get("id").getAsString());
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
                    if (!respuesta.get("exito").getAsBoolean()) {
                        throw new RuntimeException(respuesta.get("mensaje").getAsString());
                    }

                    // Merge basic user data with profile data
                    JsonObject usuarioCompleto = usuarioData.deepCopy();
                    if (respuesta.has("usuario")) {
                        JsonObject perfilData = respuesta.getAsJsonObject("usuario");
                        for (Map.Entry<String, JsonElement> entry : perfilData.entrySet()) {
                            usuarioCompleto.add(entry.getKey(), entry.getValue());
                        }
                    }

                    abrirVistaSegunRol(usuarioCompleto);
                },
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", "Error al cargar perfil: " + error.getMessage(), Alert.AlertType.ERROR)
                ),
                "carga de perfil"
        );
    }

    private void abrirVistaSegunRol(JsonObject datosUsuario) {
        Platform.runLater(() -> {
            try {
                boolean esModerador = datosUsuario.has("esModerador")
                        ? datosUsuario.get("esModerador").getAsBoolean()
                        : datosUsuario.has("rol") && datosUsuario.get("rol").getAsString().equalsIgnoreCase("MODERADOR");

                String fxmlPath = esModerador
                        ? "/com/taller/estudiantevistas/fxml/moderador.fxml"
                        : "/com/taller/estudiantevistas/fxml/perfil.fxml";

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();

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
                manejarError("cargar vista de perfil", e);
            }
        });
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
        mostrarAlerta("En desarrollo", "Funcionalidad de notificaciones en desarrollo", Alert.AlertType.INFORMATION);
    }

    private void abrirChat() {
        mostrarAlerta("En desarrollo", "Funcionalidad de chat en desarrollo", Alert.AlertType.INFORMATION);
    }

    private void abrirContacto() {
        if (usuarioData == null || !usuarioData.has("id")) {
            mostrarAlerta("Error", "Datos del usuario no disponibles", Alert.AlertType.ERROR);
            return;
        }

        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_GRUPOS_ESTUDIO");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("userId", usuarioData.get("id").getAsString());
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
                        JsonArray grupos = respuesta.getAsJsonArray("grupos");
                        Platform.runLater(() -> mostrarVistaContacto(grupos));
                    } else {
                        throw new RuntimeException(respuesta.get("mensaje").getAsString());
                    }
                },
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", "Error al obtener grupos: " + error.getMessage(), Alert.AlertType.ERROR)
                ),
                "obtención de grupos de estudio"
        );
    }

    private void mostrarVistaContacto(JsonArray grupos) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/contacto.fxml"));
            Parent root = loader.load();

            ControladorContacto controlador = loader.getController();
            controlador.inicializar(grupos, usuarioData, cliente);

            Stage stage = new Stage();
            stage.setTitle("Contactar Grupos de Estudio");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            manejarError("mostrar vista de contacto", e);
        }
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public void addActualizacionListener(ActualizacionListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    /////////
    /**
     * Crea un ítem de contenido educativo con nuevo estilo
     */
    private Node crearItemContenido(JsonObject contenido) {
        VBox item = new VBox(8);
        item.getStyleClass().add("contenido-item");

        // Título con icono según tipo
        HBox tituloBox = new HBox(5);
        tituloBox.setAlignment(Pos.CENTER_LEFT);

        String tipoContenido = contenido.get("tipo").getAsString();
        Label iconoTipo = new Label(obtenerIconoTipo(tipoContenido));
        iconoTipo.setStyle("-fx-font-size: 16px;");

        Label titulo = new Label(contenido.get("titulo").getAsString());
        titulo.getStyleClass().add("contenido-titulo");

        tituloBox.getChildren().addAll(iconoTipo, titulo);

        // Metadatos
        HBox metadatos = new HBox(10);
        metadatos.getStyleClass().add("contenido-metadatos");

        Label autor = crearMetadataLabel("👤 " + contenido.get("autor").getAsString());
        Label fecha = crearMetadataLabel("📅 " + formatFechaContenido(contenido.get("fechaCreacion").getAsString()));
        Label tema = crearMetadataLabel("🏷 " + contenido.get("tema").getAsString());

        metadatos.getChildren().addAll(autor, fecha, tema);

        // Descripción
        Text descripcion = new Text(contenido.get("descripcion").getAsString());
        descripcion.getStyleClass().add("descripcion-text");
        descripcion.setWrappingWidth(panelContenidos.getWidth() - 40);

        // Contenido principal
        Node contenidoVisual = crearVisualizacionContenido(contenido);

        item.getChildren().addAll(tituloBox, metadatos, descripcion, contenidoVisual);

        // Efecto de hover
        item.setOnMouseEntered(e -> item.setStyle("-fx-border-color: #bdc3c7;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-border-color: #e0e0e0;"));
        item.setOnMouseClicked(e -> abrirVistaContenido(contenido));
        return item;
    }

    private void abrirVistaContenido(JsonObject contenido) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/contenido-layout.fxml"));
            Parent root = loader.load();

            ControladorContenido controlador = loader.getController();
            controlador.inicializar(contenido, cliente, usuarioData);

            Stage stage = new Stage();
            stage.setTitle("Detalles del Contenido");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            manejarError("abrir vista de contenido", e);
        }
    }

    private Label crearMetadataLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metadata-label");
        return label;
    }

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

    /**
     * Crea un ítem de solicitud de ayuda con nuevo estilo
     */
    /**
    private Node crearItemSolicitud(JsonObject solicitud) {
        VBox item = new VBox(8);
        item.getStyleClass().add("solicitud-item");

        // Título
        Label temaLabel = new Label(solicitud.get("tema").getAsString());
        temaLabel.getStyleClass().add("solicitud-titulo");

        // Estados y urgencia
        HBox estadosBox = new HBox(10);
        estadosBox.setAlignment(Pos.CENTER_LEFT);

        String urgencia = solicitud.get("urgencia").getAsString();
        String estado = solicitud.get("estado").getAsString();

        Label urgenciaLabel = new Label("🔺 " + urgencia);
        urgenciaLabel.getStyleClass().add("urgencia-" + urgencia.toLowerCase());

        Label estadoLabel = new Label("◉ " + estado);
        estadoLabel.getStyleClass().add("estado-" + estado.toLowerCase());

        estadosBox.getChildren().addAll(urgenciaLabel, estadoLabel);

        // Descripción
        Text descripcion = new Text(solicitud.get("descripcion").getAsString());
        descripcion.getStyleClass().add("descripcion-text");
        descripcion.setWrappingWidth(panelSolicitudes.getWidth() - 40);

        // Footer
        HBox footer = new HBox(10);
        footer.getStyleClass().add("solicitud-footer");

        String fechaStr = formatFecha(solicitud.get("fecha").getAsLong());
        String solicitante = obtenerNombreSolicitante(solicitud);

        Label fechaLabel = crearMetadataLabel("📅 " + fechaStr);
        Label solicitanteLabel = crearMetadataLabel("👤 " + solicitante);

        footer.getChildren().addAll(fechaLabel, solicitanteLabel);

        item.getChildren().addAll(temaLabel, estadosBox, descripcion, footer);

        // Efecto de hover
        item.setOnMouseEntered(e -> item.setStyle("-fx-border-color: #bdc3c7;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-border-color: #e0e0e0;"));

        return item;
    }*/

    private void mostrarContenidosEnPanel(JsonArray contenidos, Pane panel) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(10));
        contenedor.getStyleClass().add("contenido-panel");
        contenedor.setMaxWidth(panel.getWidth() - 20);

        if (esMensajeEspecial(contenidos)) {
            JsonObject mensaje = contenidos.get(0).getAsJsonObject();
            contenedor.getChildren().add(crearMensajeUI(
                    mensaje.get("titulo").getAsString(),
                    mensaje.get("detalle").getAsString(),
                    this::recargarContenidos
            ));
        } else {
            Set<String> idsMostrados = new HashSet<>();
            for (JsonElement elemento : contenidos) {
                JsonObject contenido = elemento.getAsJsonObject();
                String id = contenido.get("id").getAsString();

                if (!idsMostrados.contains(id)) {
                    contenedor.getChildren().add(crearItemContenido(contenido));
                    idsMostrados.add(id);
                }
            }
        }

        ScrollPane scrollPane = crearScrollPane(contenedor);
        scrollPane.setPrefViewportWidth(panel.getWidth() - 15);
        panel.getChildren().clear();
        panel.getChildren().add(scrollPane);
    }
/**
    private void mostrarSolicitudesEnPanel(JsonArray solicitudes, Pane panel) {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(15));
        contenedor.getStyleClass().add("solicitud-panel");

        if (esMensajeEspecial(solicitudes)) {
            JsonObject mensaje = solicitudes.get(0).getAsJsonObject();
            contenedor.getChildren().add(crearMensajeUI(
                    mensaje.get("titulo").getAsString(),
                    mensaje.get("detalle").getAsString(),
                    this::recargarSolicitudes
            ));
        } else {
            Set<String> idsMostrados = new HashSet<>();
            for (JsonElement elemento : solicitudes) {
                JsonObject solicitud = elemento.getAsJsonObject();
                String id = solicitud.get("id").getAsString();

                if (!idsMostrados.contains(id)) {
                    contenedor.getChildren().add(crearItemSolicitud(solicitud));
                    idsMostrados.add(id);
                }
            }
        }

        ScrollPane scrollPane = crearScrollPane(contenedor);
        panel.getChildren().clear();
        panel.getChildren().add(scrollPane);
    }
*/
private ScrollPane crearScrollPane(Node contenido) {
    ScrollPane scrollPane = new ScrollPane(contenido);
    scrollPane.setFitToWidth(true);
    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

    // Ajustar el padding y márgenes para que ocupe menos espacio
    scrollPane.setPadding(new Insets(0));
    scrollPane.setPrefViewportWidth(panelContenidos.getWidth() - 15); // 15px menos que el panel padre

    // Aplicar estilos CSS
    scrollPane.getStyleClass().add("scroll-pane");

    return scrollPane;
}

}