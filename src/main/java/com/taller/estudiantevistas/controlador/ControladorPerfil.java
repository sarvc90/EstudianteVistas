package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ControladorPerfil {

    private static final Logger LOGGER = Logger.getLogger(ControladorPerfil.class.getName());
    private JsonObject datosUsuario;
    private ClienteServicio cliente;
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FXML private ImageView imgPerfil;
    @FXML private Label lblNombres, lblCorreo, lblIntereses;
    @FXML private Button btnVerContenidos;
    @FXML private Button btnVerSugerencias;
    @FXML private Button btnBuscarGrupos;
    @FXML private Button btnVerSolicitudes;
    @FXML private Button btnPublicarAyuda;
    @FXML private Button btnPublicarContenido;
    @FXML private ComboBox<String> comboGruposEstudio;

    @FXML
    public void initialize() {
        // Configurar acciones de los botones
        btnVerContenidos.setOnAction(e -> manejarVerContenidos());
        btnVerSugerencias.setOnAction(e -> manejarVerSugerencias());
        btnBuscarGrupos.setOnAction(e -> manejarBuscarGrupos());
        btnVerSolicitudes.setOnAction(e -> manejarVerSolicitudes());
        btnPublicarAyuda.setOnAction(e -> abrirVistaCrearSolicitud());
        btnPublicarContenido.setOnAction(e -> abrirVistaCrearPublicacion());
    }

    public void inicializar(JsonObject datosUsuario, ClienteServicio cliente) {
        this.datosUsuario = datosUsuario;
        this.cliente = cliente;
        if (datosUsuario != null) {
            Platform.runLater(() -> {
                lblNombres.setText(datosUsuario.get("nombres").getAsString());
                lblCorreo.setText(datosUsuario.get("correo").getAsString());
                lblIntereses.setText(datosUsuario.get("intereses").getAsString());
                cargarGruposEstudio(datosUsuario.get("id").getAsString());
            });
        }
    }

    private void cargarGruposEstudio(String userId) {
        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_GRUPOS_ESTUDIO");
                    JsonObject datos = new JsonObject();
                    datos.addProperty("userId", userId);
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
                        JsonArray gruposJson = respuesta.getAsJsonArray("grupos");
                        for (JsonElement grupo : gruposJson) {
                            String nombreGrupo = grupo.getAsJsonObject().get("nombre").getAsString();
                            // Agregar solo si no está ya en el ComboBox
                            if (!comboGruposEstudio.getItems().contains(nombreGrupo)) {
                                comboGruposEstudio.getItems().add(nombreGrupo);
                            }
                        }
                    } else {
                        mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR);
                    }
                },
                "carga de grupos de estudio"
        );
    }


    private void obtenerDatosUsuarioCompletos(String userId) {
        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_DATOS_PERFIL");
                    solicitud.addProperty("userId", userId);
                    cliente.getSalida().println(solicitud.toString());
                    try {
                        String respuesta = cliente.getEntrada().readLine();
                        if (respuesta == null) {
                            throw new IOException("El servidor no respondió");
                        }
                        return respuesta;
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación: " + e.getMessage(), e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject datosCompletos = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (datosCompletos.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> actualizarUI(datosCompletos.getAsJsonObject("datosUsuario")));
                        } else {
                            Platform.runLater(() -> mostrarAlerta("Error", datosCompletos.get("mensaje").getAsString(), Alert.AlertType.ERROR));
                        }
                    } catch (JsonParseException e) {
                        Platform.runLater(() -> mostrarAlerta("Error", "Respuesta del servidor no válida", Alert.AlertType.ERROR));
                    }
                },
                "obtención de datos de perfil"
        );
    }

    private void actualizarUI(JsonObject datosUsuario) {
        if (datosUsuario.has("gruposEstudio")) {
            JsonArray gruposJson = datosUsuario.get("gruposEstudio").getAsJsonArray();
            String[] grupos = new String[gruposJson.size()];
            for (int i = 0; i < gruposJson.size(); i++) {
                grupos[i] = gruposJson.get(i).getAsString();
            }
            comboGruposEstudio.getItems().setAll(grupos);
        }
    }

    private void abrirVistaCrearSolicitud() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/crear-solicitud.fxml"));
            Parent root = loader.load();

            ControladorSolicitudAyuda controlador = loader.getController();
            controlador.inicializar(datosUsuario, cliente);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Crear Solicitud de Ayuda");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir la vista de creación de solicitud", Alert.AlertType.ERROR);
        }
    }

    private void abrirVistaCrearPublicacion() {
        try {
            String fxmlPath = "/com/taller/estudiantevistas/fxml/crear-publicacion.fxml";
            URL resourceUrl = getClass().getResource(fxmlPath);

            if (resourceUrl == null) {
                throw new IOException("No se encontró el archivo FXML en: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            ControladorCrearPublicacion controlador = loader.getController();
            if (controlador == null) {
                throw new IOException("No se pudo obtener el controlador de la publicación");
            }
            controlador.inicializar(datosUsuario, cliente);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Crear Nueva Publicación");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            LOGGER.severe("Error al abrir vista de publicación: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la vista de creación de publicación: " + e.getMessage(), Alert.AlertType.ERROR);
        }
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
            LOGGER.severe("Error en " + contexto + ": " + task.getException().getMessage());
            Platform.runLater(() -> mostrarAlerta("Error", "Error al " + contexto, Alert.AlertType.ERROR));
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

    private void manejarVerContenidos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/contenidos-perfil.fxml"));
            Parent root = loader.load();

            ControladorContenidosPerfil controlador = loader.getController();

            Stage stageActual = (Stage) btnVerContenidos.getScene().getWindow();

            controlador.inicializar(
                    datosUsuario.get("id").getAsString(),
                    cliente,
                    this::cargarContenidosDesdeServidor
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Mis Contenidos");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(stageActual);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la vista de contenidos", Alert.AlertType.ERROR);
        }
    }

    private void cargarContenidosDesdeServidor(String userId, Consumer<JsonArray> callback) {
        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_CONTENIDOS_COMPLETOS");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("userId", userId);
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
                        callback.accept(respuesta.getAsJsonArray("contenidos"));
                    } else {
                        Platform.runLater(() ->
                                mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR)
                        );
                    }
                },
                "carga de contenidos específicos"
        );
    }

    private void manejarVerSugerencias() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/sugerencias-perfil.fxml"));
            Parent root = loader.load();

            ControladorSugerenciasPerfil controlador = loader.getController();
            Stage stageActual = (Stage) btnVerSugerencias.getScene().getWindow();

            controlador.inicializar(
                    datosUsuario.get("id").getAsString(),
                    cliente,
                    this::cargarSugerenciasDesdeServidor
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Sugerencias de Compañeros");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(stageActual);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la vista de sugerencias", Alert.AlertType.ERROR);
        }
    }

    private void cargarSugerenciasDesdeServidor(String userId, Consumer<JsonArray> callback) {
        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_SUGERENCIAS");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("userId", userId);
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
                        callback.accept(respuesta.getAsJsonArray("sugerencias"));
                    } else {
                        Platform.runLater(() ->
                                mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR)
                        );
                    }
                },
                "carga de sugerencias"
        );
    }

    private void manejarBuscarGrupos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/buscar-grupos.fxml"));
            Parent root = loader.load();
            ControladorBuscarGrupos controlador = loader.getController();
            controlador.inicializar(cliente, datosUsuario.get("id").getAsString());
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Buscar Grupos de Estudio");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la vista de búsqueda de grupos", Alert.AlertType.ERROR);
        }
    }

    private void manejarVerSolicitudes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/solicitudes-perfil.fxml"));
            Parent root = loader.load();

            ControladorSolicitudesPerfil controlador = loader.getController();

            Stage stageActual = (Stage) btnVerSolicitudes.getScene().getWindow();

            controlador.inicializar(
                    datosUsuario.get("id").getAsString(),
                    cliente,
                    this::cargarSolicitudesDesdeServidor
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Mis Solicitudes");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(stageActual);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la vista de solicitudes", Alert.AlertType.ERROR);
        }
    }

    private void cargarSolicitudesDesdeServidor(String userId, Consumer<JsonArray> callback) {
        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_SOLICITUDES_USUARIO");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("userId", userId);
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
                        callback.accept(respuesta.getAsJsonArray("solicitudes"));
                    } else {
                        Platform.runLater(() ->
                                mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR)
                        );
                    }
                },
                "carga de solicitudes específicas"
        );
    }
}