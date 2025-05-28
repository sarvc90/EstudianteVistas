package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ControladorGrupoEstudio {
    @FXML private Text nombreGrupo;
    @FXML private Text descripcionGrupo;
    @FXML private Text miembrosCount;
    @FXML private Text contenidoCount;
    @FXML private Text actividadCount;
    @FXML private Text temaTitulo;
    @FXML private Text temaDescripcion;
    @FXML private ListView<String> listaContenido;
    @FXML private VBox contenidoVBox;

    private JsonObject grupoData;
    private PrintWriter salida;
    private BufferedReader entrada;
    private Stage primaryStage;
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public void inicializar(JsonObject grupoData, PrintWriter salida, BufferedReader entrada, Stage primaryStage) {
        this.grupoData = grupoData;
        this.salida = salida;
        this.entrada = entrada;
        this.primaryStage = primaryStage;

        System.out.println("JSON recibido en ControladorGrupoEstudio: " + grupoData.toString());

        cargarDatosGrupo();
    }

    private void cargarDatosGrupo() {
        try {
            // Cargar datos básicos con valores por defecto
            nombreGrupo.setText(getSafeString(grupoData, "nombre", "Sin nombre"));
            descripcionGrupo.setText(getSafeString(grupoData, "descripcion", "Sin descripción"));

            // Estadísticas del grupo
            miembrosCount.setText(String.valueOf(getSafeJsonArray(grupoData, "miembros").size()));
            contenidoCount.setText(String.valueOf(getSafeJsonArray(grupoData, "contenidos").size()));
            actividadCount.setText(String.valueOf(getSafeInt(grupoData, "activos", 0)));

            // Información del tema
            temaTitulo.setText(getSafeString(grupoData, "tema", "Sin tema"));
            temaDescripcion.setText(getSafeString(grupoData, "temaDescripcion", "Sin descripción del tema"));

            // Cargar contenido reciente
            listaContenido.getItems().clear();
            JsonArray contenidoReciente = getSafeJsonArray(grupoData, "contenidoReciente");
            contenidoReciente.forEach(item -> {
                if (item.isJsonPrimitive()) {
                    listaContenido.getItems().add(item.getAsString());
                } else if (item.isJsonObject()) {
                    listaContenido.getItems().add(item.getAsJsonObject().toString());
                }
            });
        } catch (Exception e) {
            mostrarAlerta("Error", "Error al cargar los datos del grupo: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // Métodos auxiliares para manejo seguro de JSON
    private String getSafeString(JsonObject json, String key, String defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : defaultValue;
    }

    private int getSafeInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsInt() : defaultValue;
    }

    private JsonArray getSafeJsonArray(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonArray() ? json.get(key).getAsJsonArray() : new JsonArray();
    }

    @FXML
    private void irAlChat() {
        mostrarAlerta("Funcionalidad en desarrollo", "El chat grupal está actualmente en desarrollo", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void unirseAlGrupo() {
        if (!grupoData.has("id") || !grupoData.has("usuarioId")) {
            mostrarAlerta("Error", "Datos incompletos para unirse al grupo", Alert.AlertType.ERROR);
            return;
        }

        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "UNIRSE_GRUPO");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("grupoId", grupoData.get("id").getAsString());
                    datos.addProperty("usuarioId", grupoData.get("usuarioId").getAsString());

                    solicitud.add("datos", datos);
                    salida.println(solicitud.toString());

                    try {
                        String respuesta = entrada.readLine();
                        return JsonParser.parseString(respuesta).getAsJsonObject();
                    } catch (IOException e) {
                        throw new RuntimeException("Error de comunicación con el servidor", e);
                    }
                },
                respuesta -> {
                    if (respuesta.get("exito").getAsBoolean()) {
                        Platform.runLater(() -> {
                            mostrarAlerta("Éxito", "Te has unido al grupo exitosamente", Alert.AlertType.INFORMATION);
                            // Actualizar contador de miembros
                            int nuevosMiembros = getSafeInt(grupoData, "miembros", 0) + 1;
                            miembrosCount.setText(String.valueOf(nuevosMiembros));
                        });
                    } else {
                        Platform.runLater(() ->
                                mostrarAlerta("Error", respuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR)
                        );
                    }
                },
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", "No se pudo completar la solicitud: " + error.getMessage(), Alert.AlertType.ERROR)
                ),
                "unirse al grupo"
        );
    }

    @FXML
    private void verMiembros() {
        if (!grupoData.has("id")) {
            mostrarAlerta("Error", "No se pudo identificar el grupo", Alert.AlertType.ERROR);
            return;
        }

        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_MIEMBROS_GRUPO");
                    JsonObject datos = new JsonObject();
                    datos.addProperty("grupoId", grupoData.get("id").getAsString());
                    solicitud.add("datos", datos);

                    salida.println(solicitud.toString());

                    try {
                        String respuesta = entrada.readLine();
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (!jsonRespuesta.get("exito").getAsBoolean()) {
                            throw new RuntimeException(jsonRespuesta.get("mensaje").getAsString());
                        }
                        return jsonRespuesta.getAsJsonArray("miembros");
                    } catch (IOException e) {
                        throw new RuntimeException("Error al obtener miembros", e);
                    } catch (JsonSyntaxException e) {
                        throw new RuntimeException("Respuesta JSON mal formada", e);
                    }
                },
                miembros -> {
                    Platform.runLater(() -> mostrarListaMiembros(miembros));
                },
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", "No se pudo obtener la lista de miembros: " + error.getMessage(), Alert.AlertType.ERROR)
                ),
                "obtener miembros del grupo"
        );
    }

    @FXML
    private void verContenido() {
        if (!grupoData.has("id")) {
            mostrarAlerta("Error", "No se pudo identificar el grupo", Alert.AlertType.ERROR);
            return;
        }

        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "OBTENER_CONTENIDO_GRUPO");
                    JsonObject datos = new JsonObject();
                    datos.addProperty("grupoId", grupoData.get("id").getAsString());
                    solicitud.add("datos", datos);

                    salida.println(solicitud.toString());

                    try {
                        String respuesta = entrada.readLine();
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (!jsonRespuesta.get("exito").getAsBoolean()) {
                            throw new RuntimeException(jsonRespuesta.get("mensaje").getAsString());
                        }
                        return jsonRespuesta.getAsJsonArray("contenido");
                    } catch (IOException e) {
                        throw new RuntimeException("Error al obtener contenido", e);
                    } catch (JsonSyntaxException e) {
                        throw new RuntimeException("Respuesta JSON mal formada", e);
                    }
                },
                contenido -> {
                    Platform.runLater(() -> mostrarListaContenido(contenido));
                },
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", "No se pudo obtener el contenido del grupo: " + error.getMessage(), Alert.AlertType.ERROR)
                ),
                "obtener contenido del grupo"
        );
    }

    @FXML
    private void verConfiguracion() {
        try {
            URL fxmlUrl = getClass().getResource("/com/taller/estudiantevistas/fxml/configuracion-grupo.fxml");
            if (fxmlUrl == null) {
                mostrarAlerta("Error", "No se pudo encontrar el archivo configuracion-grupo.fxml en la ruta especificada", Alert.AlertType.ERROR);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            ControladorConfiguracionGrupo controlador = loader.getController();
            controlador.inicializar(grupoData, salida, entrada);

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(primaryStage);
            stage.setScene(new Scene(root));
            stage.setTitle("Configuración del Grupo");
            stage.show();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo abrir la configuración: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void mostrarListaMiembros(JsonArray miembros) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Miembros del Grupo");
        alert.setHeaderText(null);
        alert.setResizable(true);

        ListView<String> listView = new ListView<>();
        miembros.forEach(m -> {
            JsonObject miembro = m.getAsJsonObject();
            String nombre = getSafeString(miembro, "nombre", "Nombre no disponible");
            String rol = getSafeString(miembro, "rol", "Sin rol definido");
            listView.getItems().add(nombre + " (" + rol + ")");
        });

        alert.getDialogPane().setContent(listView);
        alert.getDialogPane().setPrefSize(300, 400);
        alert.showAndWait();
    }

    private void mostrarListaContenido(JsonArray contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Contenido del Grupo");
        alert.setHeaderText(null);
        alert.setResizable(true);

        ListView<String> listView = new ListView<>();
        contenido.forEach(c -> {
            JsonObject item = c.getAsJsonObject();
            String titulo = getSafeString(item, "titulo", "Sin título");
            String tipo = getSafeString(item, "tipo", "Sin tipo");
            listView.getItems().add(titulo + " [" + tipo + "]");
        });

        alert.getDialogPane().setContent(listView);
        alert.getDialogPane().setPrefSize(400, 500);
        alert.showAndWait();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Platform.runLater(() -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
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
            System.err.println("Error en " + contexto + ": " + task.getException().getMessage());
            onError.accept(task.getException());
        });

        executor.execute(task);
    }
}

