package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import com.taller.estudiantevistas.dto.Contenido;
import com.taller.estudiantevistas.dto.TipoContenido;
import com.taller.estudiantevistas.dto.Valoracion;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la vista de contenido.
 * Muestra información detallada de un contenido específico y permite valoraciones.
 */

public class ControladorContenido {
    private static final Logger LOGGER = Logger.getLogger(ControladorContenido.class.getName());

    /**
     * Adaptador personalizado para manejar LocalDateTime en Gson.
     * Permite múltiples formatos de fecha y maneja excepciones.
     */
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });


    @FXML
    private Text txtTitulo;
    @FXML
    private Text txtAutor;
    @FXML
    private Text txtFechaPublicacion;
    @FXML
    private Text txtTema;
    @FXML
    private Text txtTipo;
    @FXML
    private TextArea txtDescripcion;
    @FXML
    private Button btnAgregarValoracion;
    @FXML
    private Button btnVerValoracionPromedio;
    @FXML
    private Button btnVerValoraciones;
    @FXML
    private Pane leftBox;
    @FXML
    private ImageView imgContenido;
    @FXML
    private VBox valoracionesContainer;
    @FXML
    private ScrollPane scrollValoraciones;


    private Contenido contenido;
    private ClienteServicio cliente;
    private JsonObject usuarioData;
    private boolean usuarioYaValoro;
    // Adaptador para LocalDateTime que maneja múltiples formatos
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Inicializa el controlador con los datos del contenido
     */
    public void inicializar(JsonObject contenidoJson, ClienteServicio cliente, JsonObject usuarioData) {
        this.cliente = cliente;
        this.usuarioData = usuarioData;

        try {

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();


            this.contenido = gson.fromJson(contenidoJson, Contenido.class);
            verificarYLimpiarDuplicados();

            if (contenido == null) {
                throw new IllegalArgumentException("El contenido no puede ser nulo");
            }

            if (contenido.getTipo() == TipoContenido.OTRO && contenido.getContenido() != null) {
                TipoContenido tipoCalculado = TipoContenido.determinarPorExtension(contenido.getContenido());
                if (tipoCalculado != TipoContenido.OTRO) {
                    contenido.setTipo(tipoCalculado);
                    LOGGER.info("Tipo corregido a: " + tipoCalculado + " para archivo: " + contenido.getContenido());
                }
            }

            Platform.runLater(() -> {
                try {
                    configurarEventos();

                    verificarValoracionUsuario();

                    cargarDatosContenido();

                    configurarVisualizacionContenido();

                    actualizarPromedio();
                    if (valoracionesContainer != null && valoracionesContainer.getParent() != null) {
                        ((Pane) valoracionesContainer.getParent()).getChildren().remove(valoracionesContainer);
                    }

                    LOGGER.info("Contenido inicializado correctamente: " + contenido.getTitulo());
                    LOGGER.info("Tipo de contenido: " + contenido.getTipo());
                    LOGGER.info("Ruta del contenido: " + contenido.getContenido());

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al inicializar UI del contenido", e);
                    mostrarAlerta("Error", "No se pudo inicializar la interfaz: " + e.getMessage(), Alert.AlertType.ERROR);

                    Label errorLabel = new Label("Error al cargar contenido:\n" + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: red;");
                    leftBox.getChildren().clear();
                    leftBox.getChildren().add(errorLabel);
                }
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar el controlador", e);
            Platform.runLater(() -> {
                mostrarAlerta("Error Crítico", "No se pudo cargar el contenido: " + e.getMessage(), Alert.AlertType.ERROR);

                Label errorLabel = new Label("Error crítico al cargar:\n" + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
                leftBox.getChildren().clear();
                leftBox.getChildren().add(errorLabel);
            });
        }
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void verificarValoracionUsuario() {
        if (usuarioData == null || !usuarioData.has("id")) return;

        String usuarioId = usuarioData.get("id").getAsString();
        usuarioYaValoro = contenido.getValoraciones().stream()
                .filter(v -> v.getAutor() != null)  // Filtrar valoraciones sin autor
                .anyMatch(v -> v.getAutor().equals(usuarioId));
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void cargarDatosContenido() {
        try {
            txtTitulo.setText(contenido.getTitulo());
            txtAutor.setText("Autor: " + contenido.getAutor());

            // Manejo seguro de la fecha
            LocalDateTime fechaPub = contenido.getFechaPublicacion();
            String fechaTexto = "Fecha no disponible";
            if (fechaPub != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'a las' HH:mm");
                fechaTexto = fechaPub.format(formatter);
            }
            txtFechaPublicacion.setText("Publicado: " + fechaTexto);

            txtTema.setText("Tema: " + contenido.getTema());
            txtTipo.setText("Tipo: " + contenido.getTipo().toString());
            txtDescripcion.setText(contenido.getDescripcion());
            txtDescripcion.setWrapText(true);

            btnAgregarValoracion.setDisable(usuarioYaValoro);
            if (usuarioYaValoro) {
                btnAgregarValoracion.setText("Ya valoraste este contenido");
                btnAgregarValoracion.setStyle("-fx-background-color: #cccccc;");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos del contenido", e);
            mostrarAlerta("Error", "No se pudieron cargar los datos del contenido", Alert.AlertType.ERROR);
        }
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void configurarVisualizacionContenido() {
        leftBox.getChildren().clear();
        leftBox.setStyle("-fx-alignment: center; -fx-padding: 10;");

        String rutaContenido = contenido.getContenido();
        if (rutaContenido == null || rutaContenido.isEmpty()) {
            mostrarNoContenidoDisponible();
            return;
        }

        if (rutaContenido.contains("|")) {
            rutaContenido = rutaContenido.split("\\|")[0].trim();
        }

        try {
            switch (contenido.getTipo()) {
                case IMAGEN:
                    cargarImagenContenido(rutaContenido);
                    break;
                case VIDEO:
                    cargarReproductorVideo(rutaContenido);
                    break;
                case DOCUMENTO:
                    cargarVisualizadorDocumento(rutaContenido);
                    break;
                case ENLACE:
                    cargarVisualizadorEnlace(rutaContenido);
                    break;
                case OTRO:
                default:
                    cargarContenidoGenerico(rutaContenido);
                    break;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al mostrar contenido", e);
            mostrarErrorContenido("Error al mostrar el contenido: " + e.getMessage());
        }
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void mostrarNoContenidoDisponible() {
        Label lblNoContenido = new Label("No hay contenido para mostrar");
        lblNoContenido.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        leftBox.getChildren().add(lblNoContenido);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void cargarContenidoExterno() {
        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20));

        Label typeLabel = new Label("Contenido " + contenido.getTipo().toString().toLowerCase());
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Hyperlink link = new Hyperlink(contenido.getContenido());
        link.setStyle("-fx-text-fill: #7a4de8; -fx-font-size: 14px;");
        link.setOnAction(e -> abrirContenidoExterno());

        Button openButton = new Button("Abrir " + contenido.getTipo().toString().toLowerCase());
        openButton.setStyle("-fx-background-color: #7a4de8; -fx-text-fill: white;");
        openButton.setOnAction(e -> abrirContenidoExterno());

        container.getChildren().addAll(typeLabel, link, openButton);
        leftBox.getChildren().add(container);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private String extraerRutaRealContenido(String contenidoRaw) {
        if (contenidoRaw.contains("|")) {
            return contenidoRaw.split("\\|")[0].trim();
        }
        return contenidoRaw.trim();
    }

    private void abrirContenidoExterno() {
        String originalPath = contenido.getContenido();
        try {
            // Verificar si es una ruta local
            File file = new File(originalPath);
            String url;

            if (file.exists()) {
                url = file.toURI().toString();
            } else if (originalPath.startsWith("http") || originalPath.startsWith("www")) {
                url = originalPath.startsWith("http") ? originalPath : "https://" + originalPath;
            } else {
                throw new FileNotFoundException("Ruta no válida: " + originalPath);
            }
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ex) {
            Platform.runLater(() -> {
                mostrarAlerta("Error", "No se pudo abrir el contenido", Alert.AlertType.ERROR);
                Label errorLabel = new Label("Error al abrir:\n" + originalPath);
                errorLabel.setStyle("-fx-text-fill: red;");
                leftBox.getChildren().add(errorLabel);
            });
            LOGGER.log(Level.SEVERE, "Error al abrir contenido externo", ex);
        }
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void cargarContenidoGenerico(String rutaContenido) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));

        Label typeLabel = new Label("Contenido de tipo: " + contenido.getTipo());
        typeLabel.setStyle("-fx-font-weight: bold;");

        TextArea contentArea = new TextArea(rutaContenido);
        contentArea.setEditable(false);
        contentArea.setWrapText(true);

        contentBox.getChildren().addAll(typeLabel, contentArea);
        scrollPane.setContent(contentBox);
        leftBox.getChildren().add(scrollPane);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void mostrarErrorContenido(String mensaje) {
        Platform.runLater(() -> {
            leftBox.getChildren().clear();
            VBox errorBox = new VBox(5);
            errorBox.setAlignment(Pos.CENTER);

            Label errorLabel = new Label(mensaje);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

            Label pathLabel = new Label(contenido.getContenido());
            pathLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            errorBox.getChildren().addAll(errorLabel, pathLabel);
            leftBox.getChildren().add(errorBox);
        });
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void cargarImagenContenido(String rutaContenido) {
        try {
            leftBox.getChildren().clear();

            if (rutaContenido == null || rutaContenido.trim().isEmpty()) {
                mostrarErrorImagen("La ruta de la imagen está vacía");
                return;
            }

            String rutaLimpia = limpiarRutaImagen(rutaContenido);

            if (rutaLimpia.startsWith("http://") || rutaLimpia.startsWith("https://")) {
                cargarImagenDesdeURL(rutaLimpia);
            } else {
                cargarImagenDesdeArchivoLocal(rutaLimpia);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar imagen", e);
            mostrarErrorImagen("Error al cargar imagen: " + e.getMessage());
        }
    }

    /**
     * Limpia la ruta de la imagen eliminando cualquier parte innecesaria.
     * Si contiene un separador '|', se toma solo la primera parte.
     */

    private String limpiarRutaImagen(String rutaOriginal) {
        if (rutaOriginal.contains("|")) {
            return rutaOriginal.split("\\|")[0].trim();
        }
        return rutaOriginal.trim();
    }

    /**
     * Carga una imagen desde una URL o un archivo local.
     * Configura el ImageView y maneja errores de carga.
     */

    private void cargarImagenDesdeURL(String url) {
        Image image = new Image(url, true);
        configurarImageView(image);
    }

    /**
     * Carga una imagen desde un archivo local.
     * Configura el ImageView y maneja errores de carga.
     */

    private void cargarImagenDesdeArchivoLocal(String rutaLocal) throws FileNotFoundException {
        File file = new File(rutaLocal);
        if (!file.exists()) {
            throw new FileNotFoundException("Archivo no encontrado: " + rutaLocal);
        }

        String url = file.toURI().toString();
        LOGGER.info("Cargando imagen desde: " + url);

        Image image = new Image(url, true);
        configurarImageView(image);
    }

    /**
     * Configura el ImageView y maneja la visualización de la imagen.
     * Incluye manejo de errores y mensajes de carga.
     */

    private void configurarImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(leftBox.getWidth() - 40);

        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent;");

        image.errorProperty().addListener((obs, wasError, isNowError) -> {
            if (isNowError) {
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Error al cargar imagen:\n" +
                            (image.getException() != null ?
                                    image.getException().getMessage() : "Error desconocido"));
                    errorLabel.setStyle("-fx-text-fill: red;");
                    leftBox.getChildren().add(errorLabel);
                });
            }
        });

        image.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() == 1.0) {
                Platform.runLater(() -> {
                    leftBox.getChildren().clear();
                    leftBox.getChildren().add(scrollPane);
                });
            } else if (newVal.doubleValue() < 0) {
                Platform.runLater(() -> {
                    Label loadingLabel = new Label("Error cargando imagen...");
                    loadingLabel.setStyle("-fx-text-fill: orange;");
                    leftBox.getChildren().add(loadingLabel);
                });
            }
        });

        Label loadingLabel = new Label("Cargando imagen...");
        loadingLabel.setStyle("-fx-text-fill: #666;");
        leftBox.getChildren().add(loadingLabel);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void mostrarErrorImagen(String mensaje) {
        Platform.runLater(() -> {
            leftBox.getChildren().clear();

            VBox errorBox = new VBox(5);
            errorBox.setAlignment(Pos.CENTER);

            Label errorLabel = new Label(mensaje);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

            Label pathLabel = new Label("Ruta intentada: " + contenido.getContenido());
            pathLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            errorBox.getChildren().addAll(errorLabel, pathLabel);
            leftBox.getChildren().add(errorBox);
        });
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */
    private void mostrarErrorImagen() {
        Platform.runLater(() -> {
            leftBox.getChildren().clear();
            Label errorLabel = new Label("No se pudo cargar la imagen");
            errorLabel.setStyle("-fx-text-fill: red;");
            leftBox.getChildren().add(errorLabel);
        });
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void cargarReproductorVideo(String rutaContenido) {
        VBox videoBox = new VBox(10);
        videoBox.setAlignment(Pos.CENTER);

        Label lblVideo = new Label("Video contenido:");
        Hyperlink link = new Hyperlink(rutaContenido);
        link.setOnAction(e -> abrirEnNavegador(rutaContenido));

        Button btnReproducir = new Button("Reproducir Video");
        btnReproducir.setOnAction(e -> abrirEnNavegador(rutaContenido));

        videoBox.getChildren().addAll(lblVideo, link, btnReproducir);
        leftBox.getChildren().add(videoBox);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void abrirEnNavegador(String originalUrl) {
        try {
            String processedUrl = originalUrl;

            if (!processedUrl.startsWith("http://") && !processedUrl.startsWith("https://") && !processedUrl.startsWith("file://")) {
                File file = new File(processedUrl);
                if (file.exists()) {
                    processedUrl = file.toURI().toString();
                } else {
                    throw new FileNotFoundException("Archivo no encontrado: " + processedUrl);
                }
            }

            String finalUrlToOpen = processedUrl;
            java.awt.Desktop.getDesktop().browse(new java.net.URI(finalUrlToOpen));
        } catch (Exception ex) {
            String errorUrl = originalUrl;
            Platform.runLater(() -> {
                mostrarAlerta("Error", "No se pudo abrir el contenido: " + ex.getMessage(), Alert.AlertType.ERROR);
                Label errorLabel = new Label("Error al abrir:\n" + errorUrl);
                errorLabel.setStyle("-fx-text-fill: red;");
                leftBox.getChildren().add(errorLabel);
            });
        }
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void cargarVisualizadorDocumento(String rutaContenido) {
        VBox docBox = new VBox(10);
        docBox.setAlignment(Pos.CENTER);
        docBox.setPadding(new Insets(20));

        Label lblTitulo = new Label("Documento:");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Hyperlink link = new Hyperlink(rutaContenido);
        link.setStyle("-fx-text-fill: #7a4de8; -fx-font-size: 14px;");
        link.setOnAction(e -> abrirEnNavegador(rutaContenido));

        Button btnAbrir = new Button("Abrir Documento");
        btnAbrir.setStyle("-fx-background-color: #7a4de8; -fx-text-fill: white;");
        btnAbrir.setOnAction(e -> abrirEnNavegador(rutaContenido));

        docBox.getChildren().addAll(lblTitulo, link, btnAbrir);
        leftBox.getChildren().add(docBox);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void cargarVisualizadorEnlace() {
        VBox linkBox = new VBox(10);
        linkBox.setAlignment(Pos.CENTER);
        linkBox.setPadding(new Insets(20));

        Label lblTitulo = new Label("Enlace externo:");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Hyperlink link = new Hyperlink(contenido.getContenido());
        link.setStyle("-fx-text-fill: #7a4de8; -fx-font-size: 14px;");
        link.setOnAction(e -> abrirEnNavegador(contenido.getContenido()));

        Button btnAbrir = new Button("Abrir en Navegador");
        btnAbrir.setStyle("-fx-background-color: #7a4de8; -fx-text-fill: white;");
        btnAbrir.setOnAction(e -> abrirEnNavegador(contenido.getContenido()));

        linkBox.getChildren().addAll(lblTitulo, link, btnAbrir);
        leftBox.getChildren().add(linkBox);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void cargarVisualizadorEnlace(String rutaContenido) {
        VBox linkBox = new VBox(10);
        linkBox.setAlignment(Pos.CENTER);
        linkBox.setPadding(new Insets(20));

        Label lblTitulo = new Label("Enlace externo:");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Hyperlink link = new Hyperlink(rutaContenido);
        link.setStyle("-fx-text-fill: #7a4de8; -fx-font-size: 14px;");
        link.setOnAction(e -> abrirEnNavegador(rutaContenido));

        Button btnAbrir = new Button("Abrir en Navegador");
        btnAbrir.setStyle("-fx-background-color: #7a4de8; -fx-text-fill: white;");
        btnAbrir.setOnAction(e -> abrirEnNavegador(rutaContenido));

        linkBox.getChildren().addAll(lblTitulo, link, btnAbrir);
        leftBox.getChildren().add(linkBox);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    @FXML
    private void mostrarDialogoValoracion() {
        // Verificar primero que tenemos los datos necesarios del usuario
        if (usuarioData == null || !usuarioData.has("id")) {
            mostrarAlerta("Error", "No se puede valorar sin estar autenticado", Alert.AlertType.ERROR);
            return;
        }
        Dialog<Valoracion> dialog = new Dialog<>();
        dialog.setTitle("Agregar Valoración");
        dialog.setHeaderText("Valora este contenido");

        ButtonType agregarButtonType = new ButtonType("Agregar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(agregarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        Label lblPuntuacion = new Label("Puntuación:");
        RatingControl ratingControl = new RatingControl();
        Label lblComentario = new Label("Comentario:");
        TextArea txtComentario = new TextArea();
        txtComentario.setPromptText("Escribe tu comentario...");
        txtComentario.setPrefRowCount(3);

        grid.add(lblPuntuacion, 0, 0);
        grid.add(ratingControl, 1, 0);
        grid.add(lblComentario, 0, 1);
        grid.add(txtComentario, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == agregarButtonType) {
                String nombreUsuario = usuarioData.has("nombre") ?
                        usuarioData.get("nombre").getAsString() :
                        "Usuario " + usuarioData.get("id").getAsString();

                return new Valoracion(
                        usuarioData.get("id").getAsString(),
                        nombreUsuario,
                        ratingControl.getRating(),
                        txtComentario.getText(),
                        new Date()
                );
            }
            return null;
        });

        Optional<Valoracion> result = dialog.showAndWait();
        result.ifPresent(this::agregarValoracion);
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void agregarValoracion(Valoracion valoracion) {
        if (valoracion == null || usuarioYaValoro) return;

        ejecutarTareaAsync(
                () -> {
                    JsonObject respuesta = null;
                    try {
                        respuesta = enviarValoracionAlServidor(valoracion);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    // Verificar que la respuesta contenga la valoración
                    if (!respuesta.has("valoracion")) {
                        throw new RuntimeException("Respuesta del servidor no contiene datos de valoración");
                    }

                    return respuesta.getAsJsonObject("valoracion");
                },
                valoracionJson -> Platform.runLater(() -> {
                    try {
                        // Parsear la valoración desde JSON
                        Valoracion nuevaValoracion = parsearValoracionDesdeJson(valoracionJson);

                        // Actualizar el estado
                        contenido.getValoraciones().add(nuevaValoracion);
                        usuarioYaValoro = true;

                        // Actualizar UI
                        btnAgregarValoracion.setDisable(true);
                        btnAgregarValoracion.setText("Ya valoraste este contenido");
                        btnAgregarValoracion.setStyle("-fx-background-color: #cccccc;");

                        actualizarPromedio();
                        mostrarAlerta("Éxito", "Valoración agregada correctamente", Alert.AlertType.INFORMATION);
                    } catch (Exception e) {
                        mostrarAlerta("Error", "Error al procesar valoración: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }),
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR)
                ),
                "agregar valoración"
        );
    }

    private Valoracion parsearValoracionDesdeJson(JsonObject valoracionJson) {
        try {
            // Validar campos obligatorios
            if (!valoracionJson.has("id") || !valoracionJson.has("autor") ||
                    !valoracionJson.has("puntuacion") || !valoracionJson.has("comentario")) {
                throw new IllegalArgumentException("JSON de valoración incompleto");
            }

            // Parsear la fecha con manejo de valores nulos
            Date fecha = new Date(); // Fecha actual por defecto
            if (valoracionJson.has("fecha")) {
                try {
                    if (valoracionJson.get("fecha").isJsonPrimitive() &&
                            valoracionJson.get("fecha").getAsJsonPrimitive().isNumber()) {
                        fecha = new Date(valoracionJson.get("fecha").getAsLong());
                    } else {
                        String fechaStr = valoracionJson.get("fecha").getAsString();
                        if (fechaStr != null && !fechaStr.isEmpty() && !fechaStr.equalsIgnoreCase("null")) {
                            SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            fecha = formatoFecha.parse(fechaStr);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al parsear fecha, usando fecha actual", e);
                }
            }

            return new Valoracion(
                    valoracionJson.get("id").getAsString(),
                    valoracionJson.get("autor").getAsString(),
                    valoracionJson.get("puntuacion").getAsInt(),
                    valoracionJson.get("comentario").getAsString(),
                    fecha
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al parsear valoración desde JSON", e);
            throw new RuntimeException("Error al procesar valoración: " + e.getMessage(), e);
        }
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void verificarYLimpiarDuplicados() {
        if (contenido.getContenido() != null && contenido.getContenido().contains("|")) {
            String[] partes = contenido.getContenido().split("\\|");
            if (partes.length > 1) {
                // Conservar solo la parte del contenido (primera parte)
                contenido.setContenido(partes[0].trim());

                // Las valoraciones deben estar en la lista separada, no en el campo contenido
                LOGGER.warning("Se detectó contenido con valoraciones embebidas. Se ha limpiado el campo contenido.");
            }
        }
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    @FXML
    private void verValoracionPromedio() {
        ejecutarTareaAsync(
                this::obtenerPromedioActualizado,
                promedio -> Platform.runLater(() -> {
                    contenido.setPromedioValoraciones(promedio);
                    mostrarAlerta("Valoración Promedio",
                            String.format("El promedio actual es: %.1f ⭐", promedio),
                            Alert.AlertType.INFORMATION);
                }),
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR)
                ),
                "obtener valoración promedio"
        );
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    @FXML
    private void verValoraciones() {
        // Mostrar indicador de carga
        btnVerValoraciones.setDisable(true);
        btnVerValoraciones.setText("Cargando...");

        ejecutarTareaAsync(
                this::obtenerValoracionesActualizadas,
                respuesta -> Platform.runLater(() -> {
                    btnVerValoraciones.setDisable(false);
                    btnVerValoraciones.setText("Ver Valoraciones");

                    if (respuesta.get("exito").getAsBoolean()) {
                        mostrarDialogoValoraciones(respuesta);
                    } else {
                        mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR);
                    }
                }),
                error -> Platform.runLater(() -> {
                    btnVerValoraciones.setDisable(false);
                    btnVerValoraciones.setText("Ver Valoraciones");
                    mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR);
                }),
                "obtener valoraciones"
        );
    }

    /**
     * Configura los eventos de los botones y otros elementos de la UI.
     */

    private void mostrarDialogoValoraciones(JsonObject respuestaJson) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Valoraciones - " + contenido.getTitulo());

        VBox mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setAlignment(Pos.TOP_CENTER);

        // Estadísticas
        double promedio = respuestaJson.has("promedio") ? respuestaJson.get("promedio").getAsDouble() : 0;
        int total = respuestaJson.has("total") ? respuestaJson.get("total").getAsInt() : 0;

        Label lblStats = new Label(String.format("★ Promedio: %.1f/5.0 (%d valoraciones)", promedio, total));
        lblStats.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Contenedor para valoraciones
        VBox valoracionesContainer = new VBox(10);
        valoracionesContainer.setPadding(new Insets(10));

        if (respuestaJson.has("valoraciones") && !respuestaJson.get("valoraciones").isJsonNull()) {
            JsonArray valoraciones = respuestaJson.getAsJsonArray("valoraciones");
            if (valoraciones.size() == 0) {
                Label lblEmpty = new Label("No hay valoraciones aún");
                lblEmpty.setStyle("-fx-font-style: italic;");
                valoracionesContainer.getChildren().add(lblEmpty);
            } else {
                for (JsonElement element : valoraciones) {
                    if (!element.isJsonNull()) {
                        JsonObject valoracionJson = element.getAsJsonObject();
                        valoracionesContainer.getChildren().add(crearItemValoracion(valoracionJson));
                    }
                }
            }
        } else {
            Label lblError = new Label("No se pudieron cargar las valoraciones");
            lblError.setStyle("-fx-text-fill: red;");
            valoracionesContainer.getChildren().add(lblError);
        }

        ScrollPane scrollPane = new ScrollPane(valoracionesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> dialog.close());
        btnCerrar.setStyle("-fx-background-color: #7a4de8; -fx-text-fill: white;");

        mainContainer.getChildren().addAll(lblStats, scrollPane, btnCerrar);
        Scene scene = new Scene(mainContainer, 500, 500);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Crea un elemento visual para una valoración.
     */

    private void scheduleValoracionesRefresh() {
        // Actualizar cada 5 segundos
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> verValoraciones())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Detener la actualización cuando se cierre el diálogo
        if (valoracionesContainer.getScene() != null &&
                valoracionesContainer.getScene().getWindow() != null) {
            valoracionesContainer.getScene().getWindow().setOnHidden(e -> timeline.stop());
        }
    }

    /**
     * Crea un elemento visual para una valoración.
     */
    private void configurarEventos() {
        btnAgregarValoracion.setOnAction(e -> mostrarDialogoValoracion());
        btnVerValoracionPromedio.setOnAction(e -> verValoracionPromedio());
        btnVerValoraciones.setOnAction(e -> verValoraciones());
    }

    /**
     * Envía la valoración al servidor y maneja la respuesta.
     */

    private JsonObject enviarValoracionAlServidor(Valoracion valoracion) throws Exception {
        if (usuarioData == null) {
            throw new RuntimeException("No hay datos de usuario disponibles para enviar valoración");
        }

        if (!usuarioData.has("id")) {
            throw new RuntimeException("Falta el ID de usuario en los datos");
        }

        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "AGREGAR_VALORACION");

        JsonObject datos = new JsonObject();
        datos.addProperty("contenidoId", contenido.getId());
        datos.addProperty("usuarioId", usuarioData.get("id").getAsString());

        String nombreUsuario = usuarioData.has("nombre") ?
                usuarioData.get("nombre").getAsString() :
                "Usuario " + usuarioData.get("id").getAsString();
        datos.addProperty("usuarioNombre", nombreUsuario);

        datos.addProperty("puntuacion", valoracion.getPuntuacion());
        datos.addProperty("comentario", valoracion.getComentario());

        solicitud.add("datos", datos);

        cliente.getSalida().println(solicitud.toString());
        String respuesta = cliente.getEntrada().readLine();

        if (respuesta == null || respuesta.isEmpty()) {
            throw new RuntimeException("No se recibió respuesta del servidor");
        }

        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

        if (!jsonRespuesta.get("exito").getAsBoolean()) {
            throw new RuntimeException(
                    jsonRespuesta.has("mensaje") ?
                            jsonRespuesta.get("mensaje").getAsString() :
                            "Error desconocido al agregar valoración"
            );
        }

        return jsonRespuesta;
    }

    /**
     * Envía la valoración al servidor y maneja la respuesta.
     */

    private Double obtenerPromedioActualizado() {
        try {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "OBTENER_VALORACION"); // Cambiado a OBTENER_VALORACION

            JsonObject datos = new JsonObject();
            datos.addProperty("contenidoId", contenido.getId());

            solicitud.add("datos", datos);

            cliente.getSalida().println(solicitud.toString());
            String respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

            if (!jsonRespuesta.get("exito").getAsBoolean()) {
                throw new RuntimeException(jsonRespuesta.get("mensaje").getAsString());
            }

            return jsonRespuesta.get("promedio").getAsDouble();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener promedio de valoraciones", e);
            throw new RuntimeException("No se pudo obtener el promedio de valoraciones: " + e.getMessage());
        }
    }

    /**
     * Envía la valoración al servidor y maneja la respuesta.
     */

    private JsonObject obtenerValoracionesActualizadas() {
        try {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "OBTENER_VALORACIONES");
            JsonObject datos = new JsonObject();
            datos.addProperty("contenidoId", contenido.getId());
            solicitud.add("datos", datos);

            cliente.getSalida().println(solicitud.toString());
            String respuesta = cliente.getEntrada().readLine();

            if (respuesta == null || respuesta.isEmpty()) {
                throw new RuntimeException("No se recibió respuesta del servidor");
            }

            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

            if (!jsonRespuesta.get("exito").getAsBoolean()) {
                LOGGER.log(Level.WARNING, "Error del servidor al obtener valoraciones: {0}",
                        jsonRespuesta.get("mensaje").getAsString());
                return crearRespuestaFallback();
            }

            if (jsonRespuesta.has("valoraciones")) {
                JsonArray valoraciones = jsonRespuesta.getAsJsonArray("valoraciones");
                JsonArray valoracionesNormalizadas = new JsonArray();

                for (JsonElement element : valoraciones) {
                    JsonObject valoracion = element.getAsJsonObject();
                    normalizarValoracionJson(valoracion);
                    valoracionesNormalizadas.add(valoracion);
                }

                // Reemplazar el array por el normalizado
                jsonRespuesta.add("valoraciones", valoracionesNormalizadas);
            }

            return jsonRespuesta;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener valoraciones", e);
            return crearRespuestaFallback();
        }
    }

    /**
     * Normaliza los campos de una valoración JSON para asegurar que todos los campos necesarios estén presentes.
     * Si faltan campos, se les asigna un valor por defecto.
     */
    private void normalizarValoracionJson(JsonObject valoracion) {
        if (!valoracion.has("id")) {
            valoracion.addProperty("id", UUID.randomUUID().toString());
        }
        if (!valoracion.has("autor") || valoracion.get("autor").getAsString().isEmpty()) {
            valoracion.addProperty("autor", "Usuario anónimo");
        }
        if (!valoracion.has("puntuacion")) {
            valoracion.addProperty("puntuacion", 0);
        }
        if (!valoracion.has("comentario") || valoracion.get("comentario").getAsString().isEmpty()) {
            valoracion.addProperty("comentario", "Sin comentario");
        }
        if (!valoracion.has("fecha") || valoracion.get("fecha").isJsonNull()) {
            valoracion.addProperty("fecha", dateFormat.format(new Date()));
        }
    }

    /**
     * Crea una respuesta de fallback en caso de error al obtener valoraciones.
     * Incluye el promedio y las valoraciones del contenido.
     */
    private JsonObject crearRespuestaFallback() {
        JsonObject respuestaFallback = new JsonObject();
        respuestaFallback.addProperty("exito", true);
        respuestaFallback.addProperty("promedio", contenido.getPromedioValoraciones());
        respuestaFallback.addProperty("total", contenido.getValoraciones().size());

        JsonArray valoracionesArray = new JsonArray();
        for (Valoracion v : contenido.getValoraciones()) {
            JsonObject valoracionJson = new JsonObject();
            valoracionJson.addProperty("id", v.getId() != null ? v.getId() : UUID.randomUUID().toString());
            valoracionJson.addProperty("autor", v.getAutor() != null ? v.getAutor() : "Usuario anónimo");
            valoracionJson.addProperty("puntuacion", v.getPuntuacion());
            valoracionJson.addProperty("comentario", v.getComentario() != null ? v.getComentario() : "Sin comentario");
            valoracionJson.addProperty("fecha", v.getFecha() != null ?
                    dateFormat.format(v.getFecha()) :
                    dateFormat.format(new Date()));
            valoracionesArray.add(valoracionJson);
        }

        respuestaFallback.add("valoraciones", valoracionesArray);
        return respuestaFallback;
    }

    /**
     * Actualiza el promedio de valoraciones del contenido y el botón correspondiente.
     */

    private void actualizarPromedio() {
        double promedio = contenido.getValoraciones().stream()
                .mapToInt(Valoracion::getPuntuacion)
                .average()
                .orElse(0.0);

        contenido.setPromedioValoraciones(promedio);
        btnVerValoracionPromedio.setText(String.format("⭐ %.1f/5", promedio));
    }

    /**
     * Muestra todas las valoraciones del contenido en una ventana modal.
     * Incluye estadísticas y un botón para actualizar.
     */

    private void mostrarTodasValoraciones(JsonObject respuestaJson) {
        VBox contenedorValoraciones = new VBox(10);
        contenedorValoraciones.setPadding(new Insets(10));
        contenedorValoraciones.setAlignment(Pos.TOP_CENTER);

        if (!respuestaJson.has("valoraciones") || respuestaJson.get("valoraciones").getAsJsonArray().size() == 0) {
            Label lblEmpty = new Label("No hay valoraciones aún");
            lblEmpty.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
            contenedorValoraciones.getChildren().add(lblEmpty);
        } else {
            JsonArray valoracionesArray = respuestaJson.getAsJsonArray("valoraciones");

            Label lblEstadisticas = new Label(String.format(
                    "Valoraciones: %d • Promedio: %.1f ⭐",
                    respuestaJson.get("total").getAsInt(),
                    respuestaJson.get("promedio").getAsDouble()
            ));
            lblEstadisticas.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            contenedorValoraciones.getChildren().add(lblEstadisticas);

            for (JsonElement element : valoracionesArray) {
                JsonObject valoracionJson = element.getAsJsonObject();
                contenedorValoraciones.getChildren().add(crearItemValoracion(valoracionJson));
            }
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Valoraciones del contenido: " + contenido.getTitulo());


        Button btnActualizar = new Button("Actualizar");
        btnActualizar.setOnAction(e -> verValoraciones());
        btnActualizar.setStyle("-fx-background-color: #7a4de8; -fx-text-fill: white;");

        ScrollPane scrollPane = new ScrollPane(contenedorValoraciones);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(400);

        VBox root = new VBox(10, scrollPane, btnActualizar);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 450, 500);
        dialog.setScene(scene);
        dialog.show();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> verValoraciones())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        dialog.setOnHidden(e -> timeline.stop());
    }

    /**
     * Crea un elemento visual para una valoración.
     * Utiliza un VBox para organizar los elementos de la valoración.
     */

    private Node crearItemValoracion(JsonObject valoracionJson) {
        VBox cajaValoracion = new VBox(5);
        cajaValoracion.setPadding(new Insets(10));
        cajaValoracion.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ccc; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        // Extraer datos
        String autor = valoracionJson.has("autor") ? valoracionJson.get("autor").getAsString() : "Anónimo";
        int puntuacion = valoracionJson.has("puntuacion") ? valoracionJson.get("puntuacion").getAsInt() : 0;
        String comentario = valoracionJson.has("comentario") ? valoracionJson.get("comentario").getAsString() : "";
        String fecha = valoracionJson.has("fecha") ? valoracionJson.get("fecha").getAsString() : "";

        // Crear etiquetas
        Label lblAutor = new Label("👤 " + autor);
        lblAutor.setStyle("-fx-font-weight: bold;");

        Label lblPuntuacion = new Label("⭐ Puntuación: " + puntuacion + "/5");
        lblPuntuacion.setStyle("-fx-text-fill: #e67e22;");

        Label lblComentario = new Label(comentario);
        lblComentario.setWrapText(true);

        Label lblFecha = new Label("🕒 " + fecha);
        lblFecha.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        cajaValoracion.getChildren().addAll(lblAutor, lblPuntuacion, lblFecha, lblComentario);
        return cajaValoracion;
    }

    /**
     * Obtiene los datos del usuario autenticado.
     * Si no hay usuario autenticado, retorna null.
     */

    private JsonObject obtenerDatosUsuario(String usuarioId) {
        try {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "OBTENER_USUARIO");

            JsonObject datos = new JsonObject();
            datos.addProperty("id", usuarioId);
            solicitud.add("datos", datos);

            cliente.getSalida().println(solicitud.toString());
            String respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

            if (jsonRespuesta.get("exito").getAsBoolean()) {
                return jsonRespuesta.getAsJsonObject("usuario");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al obtener datos del usuario", e);
        }
        return null;
    }

    /**
     * Ejecuta una tarea asíncrona y maneja el éxito y error.
     * Utiliza un ExecutorService para ejecutar la tarea en un hilo separado.
     */

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess,
                                        Consumer<Throwable> onError, String contexto) {
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

    /**
     * Muestra una alerta con el título, mensaje y tipo especificado.
     */

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }



    /**
     * Control personalizado para seleccionar puntuación con estrellas
     */
    private static class RatingControl extends HBox {
        private int rating = 0;
        private final Label[] stars = new Label[5];

        public RatingControl() {
            setSpacing(2);
            setAlignment(Pos.CENTER_LEFT);

            for (int i = 0; i < stars.length; i++) {
                final int index = i;
                stars[i] = new Label("☆");
                stars[i].setStyle("-fx-font-size: 24px; -fx-text-fill: #ccc;");
                stars[i].setOnMouseEntered(e -> highlightStars(index));
                stars[i].setOnMouseExited(e -> updateStars());
                stars[i].setOnMouseClicked(e -> setRating(index + 1));

                getChildren().add(stars[i]);
            }
        }

        /**
         * Resalta las estrellas hasta el índice especificado.
         * @param upToIndex Índice hasta donde resaltar las estrellas.
         */

        private void highlightStars(int upToIndex) {
            for (int i = 0; i < stars.length; i++) {
                stars[i].setText(i <= upToIndex ? "★" : "☆");
                stars[i].setStyle("-fx-font-size: 24px; -fx-text-fill: " + (i <= upToIndex ? "gold" : "#ccc") + ";");
            }
        }

        /**
         * Actualiza las estrellas según la puntuación actual.
         */

        private void updateStars() {
            for (int i = 0; i < stars.length; i++) {
                stars[i].setText(i < rating ? "★" : "☆");
                stars[i].setStyle("-fx-font-size: 24px; -fx-text-fill: " + (i < rating ? "gold" : "#ccc") + ";");
            }
        }

        /**
         * Obtiene la puntuación actual.
         * @return Puntuación entre 0 y 5.
         */

        public int getRating() {
            return rating;
        }

        /**
         * Establece la puntuación y actualiza las estrellas.
         * @param rating Puntuación entre 0 y 5.
         */

        public void setRating(int rating) {
            this.rating = rating;
            updateStars();
        }
    }

    /**
     * Adaptador para deserializar LocalDateTime desde JSON.
     * Permite múltiples formatos de fecha y maneja casos especiales.
     */

    private static class LocalDateTimeAdapter implements JsonDeserializer<LocalDateTime> {
        private final DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };

        /** Deserializa un objeto JSON a LocalDateTime.
         * Intenta con múltiples formatos y maneja casos especiales.
         */

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT,
                                         JsonDeserializationContext context) throws JsonParseException {
            try {
                String dateStr = json.getAsString();

                if (dateStr == null || dateStr.isEmpty() ||
                        dateStr.equalsIgnoreCase("Fecha no disponible") ||
                        dateStr.equalsIgnoreCase("null")) {
                    return null;
                }

                for (DateTimeFormatter formatter : formatters) {
                    try {
                        return LocalDateTime.parse(dateStr, formatter);
                    } catch (DateTimeParseException e) {

                    }
                }

                LOGGER.warning("No se pudo parsear la fecha: " + dateStr);
                return null;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al parsear fecha: " + json.getAsString(), e);
                return null;
            }
        }
    }
}