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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ControladorModerador {

    private static final Logger LOGGER = Logger.getLogger(ControladorModerador.class.getName());
    private JsonObject datosUsuario;
    private ClienteServicio cliente;
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Componentes UI
    @FXML private ImageView imgPerfil;
    @FXML private Label lblNombres, lblCorreo, lblIntereses;
    @FXML private Button btnVerUsuarios, btnVerContenidos;
    @FXML private Button btnVerGrafo, btnFuncionalidadGrafo, btnTablaContenidos;
    @FXML private Button btnEstudiantesConexiones, btnNivelesParticipacion;

    public void inicializar(JsonObject datosUsuario, ClienteServicio cliente) {
        this.datosUsuario = datosUsuario;
        this.cliente = cliente;

        LOGGER.info("Datos de usuario recibidos en inicializar(): " + datosUsuario.toString());
        System.out.println("DEBUG: Cliente inicializado. Conexión activa: " +
                (cliente != null && cliente.getSalida() != null));

        try {
            actualizarUI(datosUsuario);
        } catch (Exception e) {
            LOGGER.severe("Error al actualizar UI: " + e.getMessage());
            Platform.runLater(() -> mostrarAlerta("Error", "Datos de usuario incompletos", Alert.AlertType.ERROR));
        }
    }

    private void actualizarUI(JsonObject datosUsuario) {
        LOGGER.info("Datos usuario recibidos en actualizarUI(): " + datosUsuario.toString());

        Platform.runLater(() -> {
            try {
                if (datosUsuario.has("nombres")) {
                    lblNombres.setText(datosUsuario.get("nombres").getAsString());
                } else {
                    lblNombres.setText("Nombre no disponible");
                    LOGGER.warning("Campo 'nombres' no encontrado");
                }

                if (datosUsuario.has("correo")) {
                    lblCorreo.setText(datosUsuario.get("correo").getAsString());
                } else {
                    lblCorreo.setText("Correo no disponible");
                    LOGGER.warning("Campo 'correo' no encontrado");
                }

                if (datosUsuario.has("intereses")) {
                    lblIntereses.setText(datosUsuario.get("intereses").getAsString());
                } else {
                    lblIntereses.setText("Intereses no disponibles");
                    LOGGER.warning("Campo 'intereses' no encontrado");
                }
            } catch (Exception e) {
                LOGGER.severe("Error al actualizar campos: " + e.getMessage());
            }
        });
    }

    @FXML
    private void manejarVerUsuarios() {
        System.out.println("DEBUG: Botón Ver Usuarios presionado");
        ejecutarTareaAsync(
                () -> {
                    try {
                        // 1. Construir la solicitud con el formato que espera el servidor
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_TODOS_USUARIOS");

                        // Agregar objeto 'datos' (requerido por el servidor)
                        JsonObject datos = new JsonObject();
                        if (datosUsuario != null && datosUsuario.has("id")) {
                            datos.addProperty("solicitanteId", datosUsuario.get("id").getAsString());
                        }
                        solicitud.add("datos", datos);

                        String solicitudStr = solicitud.toString();
                        System.out.println("DEBUG: Enviando solicitud completa al servidor: " + solicitudStr);

                        // 2. Enviar la solicitud
                        cliente.getSalida().println(solicitudStr);
                        cliente.getSalida().flush();

                        // 3. Recibir y mostrar respuesta
                        String respuesta = cliente.getEntrada().readLine();
                        System.out.println("RESPUESTA DEL SERVIDOR (USUARIOS): " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        System.err.println("ERROR en manejarVerUsuarios(): " + e.getMessage());
                        throw new RuntimeException("Error de comunicación con el servidor", e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> mostrarVistaUsuarios());
                        } else {
                            String mensajeError = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() : "Error desconocido";
                            Platform.runLater(() -> mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR procesando respuesta: " + e.getMessage());
                        Platform.runLater(() -> mostrarAlerta("Error", "Respuesta inválida del servidor", Alert.AlertType.ERROR));
                    }
                },
                "obtención de usuarios"
        );
    }

    private void mostrarVistaUsuarios() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/gestion-usuarios.fxml"));
            Parent root = loader.load();

            ControladorGestionUsuarios controlador = loader.getController();
            controlador.inicializar(datosUsuario.get("id").getAsString());

            Stage stage = new Stage();
            stage.setTitle("Gestión de Usuarios");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            manejarError("mostrar vista de usuarios", e);
        }
    }

    @FXML
    private void manejarVerContenidos() {
        System.out.println("DEBUG: Botón Ver Contenidos presionado");
        ejecutarTareaAsync(
                () -> {
                    try {
                        // 1. Construir la solicitud con el formato correcto
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_CONTENIDOS");

                        // 2. Agregar objeto 'datos' como espera el servidor
                        JsonObject datos = new JsonObject();
                        if (datosUsuario != null && datosUsuario.has("id")) {
                            datos.addProperty("solicitanteId", datosUsuario.get("id").getAsString());
                        }
                        solicitud.add("datos", datos);

                        String solicitudStr = solicitud.toString();
                        System.out.println("DEBUG: Enviando solicitud completa al servidor: " + solicitudStr);

                        // 3. Enviar la solicitud
                        cliente.getSalida().println(solicitudStr);
                        cliente.getSalida().flush();

                        // 4. Recibir respuesta
                        String respuesta = cliente.getEntrada().readLine();
                        System.out.println("RESPUESTA DEL SERVIDOR (CONTENIDOS): " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        System.err.println("ERROR en manejarVerContenidos(): " + e.getMessage());
                        throw new RuntimeException("Error de comunicación con el servidor", e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> mostrarVistaContenidos());
                        } else {
                            String mensajeError = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() : "Error desconocido";
                            Platform.runLater(() -> mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR procesando respuesta: " + e.getMessage());
                        Platform.runLater(() -> mostrarAlerta("Error", "Respuesta inválida del servidor", Alert.AlertType.ERROR));
                    }
                },
                "obtención de contenidos"
        );
    }

    private void mostrarVistaContenidos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/gestion_contenidos.fxml"));
            Parent root = loader.load();

            ControladorGestionContenidos controlador = loader.getController();
            controlador.inicializar(datosUsuario.get("id").getAsString());

            Stage stage = new Stage();
            stage.setTitle("Gestión de Contenidos");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            manejarError("mostrar vista de contenidos", e);
        }
    }

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess, String contexto) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                try {
                    return tarea.get();
                } catch (Exception e) {
                    System.err.println("ERROR en tarea asíncrona (" + contexto + "): " + e.getMessage());
                    throw e;
                }
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            String errorMsg = "Error en " + contexto + ": " + task.getException().getMessage();
            LOGGER.severe(errorMsg);
            System.err.println(errorMsg);
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

    private void manejarError(String contexto, Exception e) {
        String errorMsg = "Error en " + contexto + ": " + e.getMessage();
        LOGGER.severe(errorMsg);
        System.err.println(errorMsg);
        Platform.runLater(() -> mostrarAlerta("Error", "Error en " + contexto, Alert.AlertType.ERROR));
    }

    // Método temporal para pruebas
    public void probarConexion() {
        ejecutarTareaAsync(
                () -> {
                    JsonObject test = new JsonObject();
                    test.addProperty("tipo", "TEST");
                    String testStr = test.toString();
                    System.out.println("DEBUG: Enviando prueba al servidor: " + testStr);
                    cliente.getSalida().println(testStr);
                    cliente.getSalida().flush();
                    try {
                        return cliente.getEntrada().readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                respuesta -> System.out.println("Respuesta de prueba: " + respuesta),
                "prueba de conexión"
        );
    }

    @FXML
    private void manejarVerGrafo() {
        System.out.println("DEBUG: Botón Ver Grafo presionado");
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_GRAFO_AFINIDAD");
                        JsonObject datos = new JsonObject();
                        if (datosUsuario != null && datosUsuario.has("id")) {
                            datos.addProperty("solicitanteId", datosUsuario.get("id").getAsString());
                        }
                        solicitud.add("datos", datos);

                        String solicitudStr = solicitud.toString();
                        System.out.println("DEBUG: Enviando solicitud completa al servidor: " + solicitudStr);
                        cliente.getSalida().println(solicitudStr);
                        cliente.getSalida().flush();

                        String respuesta = cliente.getEntrada().readLine();
                        System.out.println("RESPUESTA DEL SERVIDOR (GRAFO): " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        System.err.println("ERROR en manejarVerGrafo(): " + e.getMessage());
                        throw new RuntimeException("Error de comunicación con el servidor", e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> mostrarVistaGrafo(jsonRespuesta));
                        } else {
                            String mensajeError = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() : "Error desconocido";
                            Platform.runLater(() -> mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR procesando respuesta: " + e.getMessage());
                        Platform.runLater(() -> mostrarAlerta("Error", "Respuesta inválida del servidor", Alert.AlertType.ERROR));
                    }
                },
                "obtención de grafo"
        );
    }

    private void mostrarVistaGrafo(JsonObject grafoData) {
        // Implementar la lógica para mostrar el grafo en una nueva ventana
        // Puedes usar una biblioteca de visualización de grafos como JGraphT o similar
    }

    @FXML
    private void manejarFuncionalidadGrafo() {
        // Implementar la lógica para manejar la funcionalidad del grafo
    }

    @FXML
    private void manejarTablaContenidos() {
        // Implementar la lógica para manejar la tabla de contenidos
    }

    @FXML
    private void manejarEstudiantesConexiones() {
        // Implementar la lógica para manejar las conexiones de estudiantes
    }

    @FXML
    private void manejarNivelesParticipacion() {
        // Implementar la lógica para manejar los niveles de participación
    }
}