package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import com.taller.estudiantevistas.dto.Contenido;
import com.taller.estudiantevistas.dto.TipoContenido;
import com.taller.estudiantevistas.dto.Valoracion;
import com.taller.estudiantevistas.servicio.ClienteServicio;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControladorContenido {
    private static final Logger LOGGER = Logger.getLogger(ControladorContenido.class.getName());

    // Configuración de ejecución asíncrona
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Componentes de la UI
    @FXML private Text txtTitulo;
    @FXML private Text txtAutor;
    @FXML private Text txtFechaPublicacion;
    @FXML private Text txtTema;
    @FXML private Text txtTipo;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnAgregarValoracion;
    @FXML private Button btnVerValoracionPromedio;
    @FXML private Button btnVerValoraciones;
    @FXML private Pane leftBox;
    @FXML private ImageView imgContenido;
    @FXML private VBox valoracionesContainer;
    @FXML private ScrollPane scrollValoraciones;

    // Datos
    private Contenido contenido;
    private ClienteServicio cliente;
    private JsonObject usuarioData;
    private boolean usuarioYaValoro;

    /**
     * Inicializa el controlador con los datos del contenido
     */
    public void inicializar(JsonObject contenidoJson, ClienteServicio cliente, JsonObject usuarioData) {
        this.cliente = cliente;
        this.usuarioData = usuarioData;

        try {
            // 1. Configurar el adaptador de fecha con múltiples formatos
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();

            // 2. Convertir JSON a objeto Contenido
            this.contenido = gson.fromJson(contenidoJson, Contenido.class);

            // 3. Verificar que el contenido sea válido
            if (contenido == null) {
                throw new IllegalArgumentException("El contenido no puede ser nulo");
            }

            // Corrección del tipo basado en la extensión del archivo
            if (contenido.getTipo() == TipoContenido.OTRO && contenido.getContenido() != null) {
                TipoContenido tipoCalculado = TipoContenido.determinarPorExtension(contenido.getContenido());
                if (tipoCalculado != TipoContenido.OTRO) {
                    contenido.setTipo(tipoCalculado);
                    LOGGER.info("Tipo corregido a: " + tipoCalculado + " para archivo: " + contenido.getContenido());
                }
            }

            // 4. Configuración inicial de UI
            Platform.runLater(() -> {
                try {
                    // 4.1. Configurar eventos primero para evitar problemas
                    configurarEventos();

                    // 4.2. Verificar valoración del usuario
                    verificarValoracionUsuario();

                    // 4.3. Cargar datos básicos
                    cargarDatosContenido();

                    // 4.4. Cargar el contenido específico (imagen, video, etc.)
                    configurarVisualizacionContenido();

                    // 4.5. Actualizar el promedio de valoraciones
                    actualizarPromedio();

                    // 4.6. Log de éxito
                    LOGGER.info("Contenido inicializado correctamente: " + contenido.getTitulo());
                    LOGGER.info("Tipo de contenido: " + contenido.getTipo());
                    LOGGER.info("Ruta del contenido: " + contenido.getContenido());

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al inicializar UI del contenido", e);
                    mostrarAlerta("Error", "No se pudo inicializar la interfaz: " + e.getMessage(), Alert.AlertType.ERROR);

                    // Mostrar mensaje de error en el leftBox
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

                // Mostrar mensaje de error más detallado
                Label errorLabel = new Label("Error crítico al cargar:\n" + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
                leftBox.getChildren().clear();
                leftBox.getChildren().add(errorLabel);
            });
        }
    }

    private void verificarValoracionUsuario() {
        if (usuarioData == null || !usuarioData.has("id")) return;

        String usuarioId = usuarioData.get("id").getAsString();
        usuarioYaValoro = contenido.getValoraciones().stream()
                .filter(v -> v.getAutor() != null)  // Filtrar valoraciones sin autor
                .anyMatch(v -> v.getAutor().equals(usuarioId));
    }

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

            // Configurar botón de valoración
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

    private void configurarVisualizacionContenido() {
        leftBox.getChildren().clear();
        leftBox.setStyle("-fx-alignment: center; -fx-padding: 10;");

        if (contenido.getContenido() == null || contenido.getContenido().isEmpty()) {
            Label lblNoContenido = new Label("No hay contenido para mostrar");
            lblNoContenido.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            leftBox.getChildren().add(lblNoContenido);
            return;
        }

        try {
            switch (contenido.getTipo()) {
                case IMAGEN:
                    cargarImagenContenido();
                    break;
                case VIDEO:
                case DOCUMENTO:
                case ENLACE:
                    cargarContenidoExterno();
                    break;
                case OTRO:  // Caso explícito para OTRO
                default:
                    cargarContenidoGenerico();
                    break;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al mostrar contenido", e);
            mostrarErrorContenido("Error al mostrar el contenido");
        }
    }

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

    private void abrirContenidoExterno() {
        String originalPath = contenido.getContenido();
        try {
            // Verificar si es una ruta local
            File file = new File(originalPath);
            String url;

            if (file.exists()) {
                // Convertir ruta local a URI
                url = file.toURI().toString();
            } else if (originalPath.startsWith("http") || originalPath.startsWith("www")) {
                // Es una URL web
                url = originalPath.startsWith("http") ? originalPath : "https://" + originalPath;
            } else {
                throw new FileNotFoundException("Ruta no válida: " + originalPath);
            }

            // Intentar abrir con el navegador
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

    private void cargarContenidoGenerico() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));

        Label typeLabel = new Label("Tipo de contenido: " + contenido.getTipo());
        typeLabel.setStyle("-fx-font-weight: bold;");

        TextArea contentArea = new TextArea(contenido.getContenido());
        contentArea.setEditable(false);
        contentArea.setWrapText(true);

        contentBox.getChildren().addAll(typeLabel, contentArea);
        scrollPane.setContent(contentBox);
        leftBox.getChildren().add(scrollPane);
    }

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

    private void cargarImagenContenido() {
        try {
            leftBox.getChildren().clear();

            // Convertir la ruta a formato URI
            String ruta = contenido.getContenido();
            File file = new File(ruta);

            if (!file.exists()) {
                throw new FileNotFoundException("Archivo no encontrado: " + ruta);
            }

            String url = file.toURI().toString();
            LOGGER.info("Intentando cargar imagen desde: " + url);

            Image image = new Image(url, true); // Carga asíncrona
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(leftBox.getWidth() - 40);

            // Contenedor con scroll para la imagen
            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setStyle("-fx-background: transparent;");

            // Manejar eventos de carga
            image.errorProperty().addListener((obs, wasError, isNowError) -> {
                if (isNowError) {
                    Platform.runLater(() -> {
                        Label errorLabel = new Label("Error al cargar imagen:\n" + ruta);
                        errorLabel.setStyle("-fx-text-fill: red;");
                        leftBox.getChildren().add(errorLabel);
                        LOGGER.severe("Error al cargar imagen: " + image.getException().getMessage());
                    });
                }
            });

            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0) {
                    Platform.runLater(() -> {
                        leftBox.getChildren().clear();
                        leftBox.getChildren().add(scrollPane);
                    });
                }
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar imagen", e);
            Label errorLabel = new Label("Error al cargar imagen:\n" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            leftBox.getChildren().add(errorLabel);
        }
    }

    private void mostrarErrorImagen() {
        Platform.runLater(() -> {
            leftBox.getChildren().clear();
            Label errorLabel = new Label("No se pudo cargar la imagen");
            errorLabel.setStyle("-fx-text-fill: red;");
            leftBox.getChildren().add(errorLabel);
        });
    }

    private void cargarReproductorVideo() {
        VBox videoBox = new VBox(10);
        videoBox.setAlignment(Pos.CENTER);

        Label lblVideo = new Label("Video contenido:");
        Hyperlink link = new Hyperlink(contenido.getContenido());
        link.setOnAction(e -> abrirEnNavegador(contenido.getContenido()));

        Button btnReproducir = new Button("Reproducir Video");
        btnReproducir.setOnAction(e -> abrirEnNavegador(contenido.getContenido()));

        videoBox.getChildren().addAll(lblVideo, link, btnReproducir);
        leftBox.getChildren().add(videoBox);
    }

    private void abrirEnNavegador(String originalUrl) {
        try {
            String processedUrl = originalUrl;

            // Si es una ruta local sin protocolo
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
            String errorUrl = originalUrl; // Variable final para el lambda
            Platform.runLater(() -> {
                mostrarAlerta("Error", "No se pudo abrir el contenido: " + ex.getMessage(), Alert.AlertType.ERROR);
                Label errorLabel = new Label("Error al abrir:\n" + errorUrl);
                errorLabel.setStyle("-fx-text-fill: red;");
                leftBox.getChildren().add(errorLabel);
            });
        }
    }

    private void cargarVisualizadorDocumento() {
        VBox docBox = new VBox(10);
        docBox.setAlignment(Pos.CENTER);
        docBox.setPadding(new Insets(20));

        Label lblTitulo = new Label("Documento:");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Hyperlink link = new Hyperlink(contenido.getContenido());
        link.setStyle("-fx-text-fill: #7a4de8; -fx-font-size: 14px;");
        link.setOnAction(e -> abrirEnNavegador(contenido.getContenido()));

        Button btnAbrir = new Button("Abrir Documento");
        btnAbrir.setStyle("-fx-background-color: #7a4de8; -fx-text-fill: white;");
        btnAbrir.setOnAction(e -> abrirEnNavegador(contenido.getContenido()));

        docBox.getChildren().addAll(lblTitulo, link, btnAbrir);
        leftBox.getChildren().add(docBox);
    }

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

    private void cargarContenidoTexto() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        TextArea areaTexto = new TextArea(contenido.getContenido());
        areaTexto.setEditable(false);
        areaTexto.setWrapText(true);
        areaTexto.setStyle("-fx-font-family: 'Roboto Mono', monospace; -fx-font-size: 14px;");

        scrollPane.setContent(areaTexto);
        leftBox.getChildren().add(scrollPane);
    }

    @FXML
    private void mostrarDialogoValoracion() {
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
                return new Valoracion(
                        null,
                        usuarioData.get("nombre").getAsString(),
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

    private void agregarValoracion(Valoracion valoracion) {
        ejecutarTareaAsync(
                () -> {
                    try {
                        return enviarValoracionAlServidor(valoracion);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                nuevaValoracion -> Platform.runLater(() -> {
                    contenido.getValoraciones().add(nuevaValoracion);
                    usuarioYaValoro = true;
                    btnAgregarValoracion.setDisable(true);
                    btnAgregarValoracion.setText("Ya valoraste este contenido");
                    btnAgregarValoracion.setStyle("-fx-background-color: #cccccc;");
                    actualizarPromedio();
                    mostrarAlerta("Éxito", "Valoración agregada correctamente", Alert.AlertType.INFORMATION);
                }),
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR)
                ),
                "agregar valoración"
        );
    }

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

    @FXML
    private void verValoraciones() {
        ejecutarTareaAsync(
                this::obtenerValoracionesActualizadas,
                valoraciones -> Platform.runLater(() ->
                        mostrarTodasValoraciones(valoraciones)
                ),
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", error.getMessage(), Alert.AlertType.ERROR)
                ),
                "obtener valoraciones"
        );
    }


    private void configurarEventos() {
        btnAgregarValoracion.setOnAction(e -> mostrarDialogoValoracion());
        btnVerValoracionPromedio.setOnAction(e -> verValoracionPromedio());
        btnVerValoraciones.setOnAction(e -> verValoraciones());
    }


    // ==================== MÉTODOS DE SERVICIO ====================

    private Valoracion enviarValoracionAlServidor(Valoracion valoracion) throws Exception {
        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "AGREGAR_VALORACION");

        JsonObject datos = new JsonObject();
        datos.addProperty("contenidoId", contenido.getId());
        datos.addProperty("usuarioId", usuarioData.get("id").getAsString());
        datos.addProperty("usuarioNombre", usuarioData.get("nombre").getAsString());
        datos.addProperty("puntuacion", valoracion.getPuntuacion());
        datos.addProperty("comentario", valoracion.getComentario());

        solicitud.add("datos", datos);

        cliente.getSalida().println(solicitud.toString());
        String respuesta = cliente.getEntrada().readLine();
        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

        if (!jsonRespuesta.get("exito").getAsBoolean()) {
            throw new RuntimeException(jsonRespuesta.get("mensaje").getAsString());
        }

        // Mapear la respuesta a un objeto Valoracion para el cliente
        JsonObject valoracionJson = jsonRespuesta.getAsJsonObject("valoracion");
        return new Valoracion(
                valoracionJson.get("id").getAsString(),
                valoracionJson.get("autor").getAsString(),
                valoracionJson.get("puntuacion").getAsInt(),
                valoracionJson.get("comentario").getAsString(),
                new Date(valoracionJson.get("fecha").getAsLong())
        );
    }

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

    private JsonArray obtenerValoracionesActualizadas() {
        try {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "OBTENER_VALORACIONES");

            JsonObject datos = new JsonObject();
            datos.addProperty("contenidoId", contenido.getId());

            solicitud.add("datos", datos);

            cliente.getSalida().println(solicitud.toString());
            String respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

            if (!jsonRespuesta.get("exito").getAsBoolean()) {
                throw new RuntimeException(jsonRespuesta.get("mensaje").getAsString());
            }

            return jsonRespuesta.getAsJsonArray("valoraciones");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener valoraciones", e);
            throw new RuntimeException("No se pudo obtener las valoraciones: " + e.getMessage());
        }
    }

    // ==================== MÉTODOS DE UI ====================

    private void actualizarPromedio() {
        double promedio = contenido.getValoraciones().stream()
                .mapToInt(Valoracion::getPuntuacion)
                .average()
                .orElse(0.0);

        contenido.setPromedioValoraciones(promedio);
        btnVerValoracionPromedio.setText(String.format("⭐ %.1f/5", promedio));
    }

    private void mostrarTodasValoraciones(JsonArray valoracionesJson) {
        valoracionesContainer.getChildren().clear();

        if (valoracionesJson.size() == 0) {
            Label lblEmpty = new Label("No hay valoraciones aún");
            lblEmpty.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
            valoracionesContainer.getChildren().add(lblEmpty);
            return;
        }

        Gson gson = new Gson();
        for (JsonElement elemento : valoracionesJson) {
            Valoracion valoracion = gson.fromJson(elemento, Valoracion.class);
            valoracionesContainer.getChildren().add(crearItemValoracion(valoracion));
        }

        // Mostrar el diálogo con las valoraciones
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Valoraciones del contenido");

        ScrollPane scrollPane = new ScrollPane(valoracionesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(400);

        Scene scene = new Scene(scrollPane, 450, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private Node crearItemValoracion(Valoracion valoracion) {
        VBox item = new VBox(5);
        item.getStyleClass().add("valoracion-item");
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5;");

        // Cabecera con autor y fecha
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblAutor = new Label(valoracion.getAutor());
        lblAutor.setStyle("-fx-font-weight: bold;");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Label lblFecha = new Label(sdf.format(valoracion.getFecha()));
        lblFecha.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        header.getChildren().addAll(lblAutor, lblFecha);

        // Rating con estrellas
        HBox ratingBox = new HBox(2);
        ratingBox.setAlignment(Pos.CENTER_LEFT);

        int puntuacion = valoracion.getPuntuacion();
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < puntuacion ? "★" : "☆");
            star.setStyle(i < puntuacion ? "-fx-text-fill: gold;" : "-fx-text-fill: #ccc;");
            ratingBox.getChildren().add(star);
        }

        // Comentario
        TextArea comentario = new TextArea(valoracion.getComentario());
        comentario.setEditable(false);
        comentario.setWrapText(true);
        comentario.setPrefRowCount(2);
        comentario.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        item.getChildren().addAll(header, ratingBox, comentario);
        return item;
    }

    // ==================== UTILIDADES ====================

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

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ==================== CLASES INTERNAS ====================

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

        private void highlightStars(int upToIndex) {
            for (int i = 0; i < stars.length; i++) {
                stars[i].setText(i <= upToIndex ? "★" : "☆");
                stars[i].setStyle("-fx-font-size: 24px; -fx-text-fill: " + (i <= upToIndex ? "gold" : "#ccc") + ";");
            }
        }

        private void updateStars() {
            for (int i = 0; i < stars.length; i++) {
                stars[i].setText(i < rating ? "★" : "☆");
                stars[i].setStyle("-fx-font-size: 24px; -fx-text-fill: " + (i < rating ? "gold" : "#ccc") + ";");
            }
        }

        public int getRating() {
            return rating;
        }

        public void setRating(int rating) {
            this.rating = rating;
            updateStars();
        }
    }

    private static class LocalDateTimeAdapter implements JsonDeserializer<LocalDateTime> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT,
                                         JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }
}