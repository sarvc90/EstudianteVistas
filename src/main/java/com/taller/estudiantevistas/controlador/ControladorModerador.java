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
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_TODOS_USUARIOS");
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
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_CONTENIDOS");
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
        try {
            JsonArray nodos = grafoData.getAsJsonObject("grafo").getAsJsonArray("nodos");
            JsonArray aristas = grafoData.getAsJsonObject("grafo").getAsJsonArray("aristas");

            int width = 800;
            int height = 600;

            Canvas canvas = new Canvas(width, height);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // 🎨 Fondo oscuro
            gc.setFill(Color.web("#1e1e1e"));  // gris oscuro tipo editor
            gc.fillRect(0, 0, width, height);

            Map<String, Double[]> posiciones = new HashMap<>();

            // Distribuir nodos en círculo
            int radio = 200;
            double centerX = width / 2.0;
            double centerY = height / 2.0;
            int totalNodos = nodos.size();

            for (int i = 0; i < totalNodos; i++) {
                JsonObject nodo = nodos.get(i).getAsJsonObject();
                String id = nodo.get("id").getAsString();

                double angle = 2 * Math.PI * i / totalNodos;
                double x = centerX + radio * Math.cos(angle);
                double y = centerY + radio * Math.sin(angle);

                posiciones.put(id, new Double[]{x, y});
            }

            // Dibujar aristas
            gc.setStroke(Color.LIGHTGRAY);
            gc.setLineWidth(2);
            for (JsonElement aristaElem : aristas) {
                JsonObject arista = aristaElem.getAsJsonObject();
                String origen = arista.get("origen").getAsString();
                String destino = arista.get("destino").getAsString();
                int peso = arista.get("peso").getAsInt();

                Double[] posOrigen = posiciones.get(origen);
                Double[] posDestino = posiciones.get(destino);

                gc.strokeLine(posOrigen[0], posOrigen[1], posDestino[0], posDestino[1]);

                double midX = (posOrigen[0] + posDestino[0]) / 2;
                double midY = (posOrigen[1] + posDestino[1]) / 2;
                gc.setFill(Color.ORANGE);
                gc.fillText(String.valueOf(peso), midX, midY);
            }

            // Dibujar nodos
            for (JsonElement nodoElem : nodos) {
                JsonObject nodo = nodoElem.getAsJsonObject();
                String id = nodo.get("id").getAsString();
                String nombre = nodo.get("nombre").getAsString();

                Double[] pos = posiciones.get(id);
                double x = pos[0];
                double y = pos[1];

                gc.setFill(Color.web("#7b5dd9")); // morado suave
                gc.fillOval(x - 15, y - 15, 30, 30);

                gc.setFill(Color.WHITE);
                gc.fillText(nombre, x - 25, y - 25);
            }

            // Mostrar en ventana
            Stage stage = new Stage();
            stage.setTitle("Grafo de Afinidad - Visualización");
            StackPane root = new StackPane(canvas);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            manejarError("mostrar grafo de afinidad (Canvas)", e);
        }
    }






    @FXML
    private void manejarFuncionalidadGrafo() {
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_GRAFO_AFINIDAD");
                        JsonObject datos = new JsonObject();
                        if (datosUsuario != null && datosUsuario.has("id")) {
                            datos.addProperty("solicitanteId", datosUsuario.get("id").getAsString());
                            datos.addProperty("analisis", "completo");
                        }
                        solicitud.add("datos", datos);

                        String solicitudStr = solicitud.toString();
                        cliente.getSalida().println(solicitudStr);
                        cliente.getSalida().flush();

                        return cliente.getEntrada().readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación con el servidor", e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            JsonObject grafoData = jsonRespuesta.getAsJsonObject("grafo");

                            // Mostrar análisis en una ventana emergente
                            Platform.runLater(() -> {
                                TextArea textArea = new TextArea();
                                textArea.setEditable(false);
                                textArea.setWrapText(true);

                                // Construir texto de análisis
                                StringBuilder sb = new StringBuilder();
                                sb.append("ANÁLISIS DEL GRAFO DE AFINIDAD\n\n");
                                sb.append("Total estudiantes: ").append(grafoData.get("totalEstudiantes").getAsInt()).append("\n");
                                sb.append("Total conexiones: ").append(grafoData.get("totalConexiones").getAsInt()).append("\n");
                                sb.append("Afinidad promedio: ").append(grafoData.get("afinidadPromedio").getAsDouble()).append("\n\n");

                                if (grafoData.has("comunidades")) {
                                    sb.append("COMUNIDADES DETECTADAS:\n");
                                    JsonArray comunidades = grafoData.getAsJsonArray("comunidades");
                                    for (JsonElement comunidad : comunidades) {
                                        JsonObject com = comunidad.getAsJsonObject();
                                        sb.append("- Comunidad ").append(com.get("id").getAsString())
                                                .append(": ").append(com.get("miembros").getAsInt())
                                                .append(" miembros, afinidad interna: ")
                                                .append(com.get("afinidad").getAsDouble()).append("\n");
                                    }
                                }

                                if (grafoData.has("estudiantesAislados")) {
                                    sb.append("\nESTUDIANTES AISLADOS:\n");
                                    JsonArray aislados = grafoData.getAsJsonArray("estudiantesAislados");
                                    for (JsonElement aislado : aislados) {
                                        sb.append("- ").append(aislado.getAsString()).append("\n");
                                    }
                                }

                                textArea.setText(sb.toString());

                                Stage stage = new Stage();
                                stage.setTitle("Análisis del Grafo de Afinidad");
                                stage.setScene(new Scene(textArea, 500, 400));
                                stage.show();
                            });
                        } else {
                            String mensajeError = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() : "Error desconocido";
                            Platform.runLater(() -> mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> mostrarAlerta("Error", "Error procesando análisis del grafo", Alert.AlertType.ERROR));
                    }
                },
                "análisis del grafo"
        );
    }

    @FXML
    private void manejarTablaContenidos() {
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_CONTENIDOS_COMPLETOS");
                        JsonObject datos = new JsonObject();
                        if (datosUsuario != null && datosUsuario.has("id")) {
                            datos.addProperty("userId", datosUsuario.get("id").getAsString()); // <--- CAMBIO AQUÍ

                        }
                        solicitud.add("datos", datos);

                        String solicitudStr = solicitud.toString();
                        cliente.getSalida().println(solicitudStr);
                        cliente.getSalida().flush();

                        return cliente.getEntrada().readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación con el servidor", e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> mostrarTablaContenidos(jsonRespuesta));
                        } else {
                            String mensajeError = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() : "Error desconocido";
                            Platform.runLater(() -> mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> mostrarAlerta("Error", "Error procesando tabla de contenidos", Alert.AlertType.ERROR));
                    }
                },
                "obtención de tabla de contenidos"
        );
    }

    private void mostrarTablaContenidos(JsonObject datosContenidos) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/tabla_contenidos.fxml"));
            Parent root = loader.load();

            ControladorTablaContenidos controlador = loader.getController();
            controlador.inicializar(datosContenidos);

            Stage stage = new Stage();
            stage.setTitle("Tabla de Contenidos");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            manejarError("mostrar tabla de contenidos", e);
        }
    }

    @FXML
    private void manejarEstudiantesConexiones() {
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_ESTUDIANTES_CONEXIONES");
                        JsonObject datos = new JsonObject();
                        if (datosUsuario != null && datosUsuario.has("id")) {
                            datos.addProperty("moderadorId", datosUsuario.get("id").getAsString());
                        }
                        solicitud.add("datos", datos);

                        String solicitudStr = solicitud.toString();
                        cliente.getSalida().println(solicitudStr);
                        cliente.getSalida().flush();

                        return cliente.getEntrada().readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación con el servidor", e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> mostrarEstudiantesConexiones(jsonRespuesta));
                        } else {
                            String mensajeError = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() : "Error desconocido";
                            Platform.runLater(() -> mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> mostrarAlerta("Error", "Error procesando conexiones", Alert.AlertType.ERROR));
                    }
                },
                "obtención de conexiones entre estudiantes"
        );
    }

    private void mostrarEstudiantesConexiones(JsonObject datosConexiones) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/estudiantes_conexiones.fxml"));
            Parent root = loader.load();

            ControladorEstudiantesConexiones controlador = loader.getController();
            controlador.inicializar(datosConexiones);

            Stage stage = new Stage();
            stage.setTitle("Estudiantes y sus Conexiones");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            manejarError("mostrar conexiones entre estudiantes", e);
        }
    }

    @FXML
    private void manejarNivelesParticipacion() {
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_NIVELES_PARTICIPACION");
                        JsonObject datos = new JsonObject();
                        if (datosUsuario != null && datosUsuario.has("id")) {
                            datos.addProperty("moderadorId", datosUsuario.get("id").getAsString());
                        }
                        solicitud.add("datos", datos);

                        String solicitudStr = solicitud.toString();
                        cliente.getSalida().println(solicitudStr);
                        cliente.getSalida().flush();

                        return cliente.getEntrada().readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación con el servidor", e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            Platform.runLater(() -> mostrarNivelesParticipacion(jsonRespuesta));
                        } else {
                            String mensajeError = jsonRespuesta.has("mensaje") ?
                                    jsonRespuesta.get("mensaje").getAsString() : "Error desconocido";
                            Platform.runLater(() -> mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> mostrarAlerta("Error", "Error procesando niveles de participación", Alert.AlertType.ERROR));
                    }
                },
                "obtención de niveles de participación"
        );
    }

    private void mostrarNivelesParticipacion(JsonObject datosParticipacion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/niveles_participacion.fxml"));
            Parent root = loader.load();

            ControladorNivelesParticipacion controlador = loader.getController();
            controlador.inicializar(datosParticipacion);

            Stage stage = new Stage();
            stage.setTitle("Niveles de Participación");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            manejarError("mostrar niveles de participación", e);
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
}