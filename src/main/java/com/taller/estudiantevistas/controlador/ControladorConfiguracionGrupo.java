package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ControladorConfiguracionGrupo {
    @FXML private Label nombreGrupo;
    @FXML private TextField txtNombreGrupo;
    @FXML private TextArea txtDescripcionGrupo;
    @FXML private Button btnGuardarCambios;
    @FXML private ListView<String> listaContenido;
    @FXML private Button btnEliminar;

    private JsonObject grupoData;
    private PrintWriter salida;
    private BufferedReader entrada;
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public void inicializar(JsonObject grupoData, PrintWriter salida, BufferedReader entrada) {
        this.grupoData = grupoData;
        this.salida = salida;
        this.entrada = entrada;
        cargarDatos();
    }

    private void cargarDatos() {
        // Cargar nombre con verificación de nulidad
        if (grupoData.has("nombre") && !grupoData.get("nombre").isJsonNull()) {
            String nombre = grupoData.get("nombre").getAsString();
            nombreGrupo.setText(nombre);
            txtNombreGrupo.setText(nombre);
        }

        // Cargar descripción con verificación de nulidad
        if (grupoData.has("descripcion") && !grupoData.get("descripcion").isJsonNull()) {
            txtDescripcionGrupo.setText(grupoData.get("descripcion").getAsString());
        } else {
            txtDescripcionGrupo.setText("");
        }

        // Cargar contenido del grupo
        listaContenido.getItems().clear();
        if (grupoData.has("contenidos") && !grupoData.get("contenidos").isJsonNull()) {
            JsonArray contenidos = grupoData.get("contenidos").getAsJsonArray();
            contenidos.forEach(c -> {
                if (!c.isJsonNull()) {
                    JsonObject contenido = c.getAsJsonObject();
                    if (contenido.has("titulo") && !contenido.get("titulo").isJsonNull()) {
                        listaContenido.getItems().add(contenido.get("titulo").getAsString());
                    }
                }
            });
        }
    }

    @FXML
    private void guardarCambios() {
        String nuevoNombre = txtNombreGrupo.getText().trim();
        String nuevaDescripcion = txtDescripcionGrupo.getText().trim();

        if (nuevoNombre.isEmpty()) {
            mostrarAlerta("Error", "El nombre del grupo no puede estar vacío", Alert.AlertType.ERROR);
            return;
        }

        // Verificar que tenemos los IDs necesarios
        if (!grupoData.has("id") || grupoData.get("id").isJsonNull()) {
            mostrarAlerta("Error", "No se pudo identificar el grupo", Alert.AlertType.ERROR);
            return;
        }

        String grupoId = grupoData.get("id").getAsString();
        String usuarioId = grupoData.has("usuarioId") && !grupoData.get("usuarioId").isJsonNull()
                ? grupoData.get("usuarioId").getAsString()
                : "";

        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "ACTUALIZAR_GRUPO");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("grupoId", grupoId);
                    datos.addProperty("nombre", nuevoNombre);
                    datos.addProperty("descripcion", nuevaDescripcion);
                    datos.addProperty("usuarioId", usuarioId);

                    solicitud.add("datos", datos);
                    salida.println(solicitud.toString());

                    String respuesta = null;
                    try {
                        respuesta = entrada.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return JsonParser.parseString(respuesta).getAsJsonObject();
                },
                respuesta -> {
                    if (respuesta.get("exito").getAsBoolean()) {
                        Platform.runLater(() -> {
                            mostrarAlerta("Éxito", "Cambios guardados correctamente", Alert.AlertType.INFORMATION);
                            // Actualizar los datos locales
                            grupoData.addProperty("nombre", nuevoNombre);
                            grupoData.addProperty("descripcion", nuevaDescripcion);
                            nombreGrupo.setText(nuevoNombre);
                        });
                    } else {
                        Platform.runLater(() ->
                                mostrarAlerta("Error",
                                        respuesta.has("mensaje") ? respuesta.get("mensaje").getAsString() : "Error desconocido",
                                        Alert.AlertType.ERROR)
                        );
                    }
                },
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", "No se pudieron guardar los cambios: " + error.getMessage(), Alert.AlertType.ERROR)
                ),
                "actualizar información del grupo"
        );
    }

    @FXML
    private void eliminarContenido() {
        String seleccionado = listaContenido.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Error", "Selecciona un contenido para eliminar", Alert.AlertType.ERROR);
            return;
        }

        String contenidoId = obtenerIdContenido(seleccionado);
        if (contenidoId == null) {
            mostrarAlerta("Error", "No se pudo identificar el contenido seleccionado", Alert.AlertType.ERROR);
            return;
        }

        // Verificar que tenemos los IDs necesarios
        if (!grupoData.has("id") || grupoData.get("id").isJsonNull()) {
            mostrarAlerta("Error", "No se pudo identificar el grupo", Alert.AlertType.ERROR);
            return;
        }

        String grupoId = grupoData.get("id").getAsString();
        String usuarioId = grupoData.has("usuarioId") && !grupoData.get("usuarioId").isJsonNull()
                ? grupoData.get("usuarioId").getAsString()
                : "";

        ejecutarTareaAsync(
                () -> {
                    JsonObject solicitud = new JsonObject();
                    solicitud.addProperty("tipo", "ELIMINAR_CONTENIDO_GRUPO");

                    JsonObject datos = new JsonObject();
                    datos.addProperty("grupoId", grupoId);
                    datos.addProperty("contenidoId", contenidoId);
                    datos.addProperty("usuarioId", usuarioId);

                    solicitud.add("datos", datos);
                    salida.println(solicitud.toString());

                    String respuesta = null;
                    try {
                        respuesta = entrada.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return JsonParser.parseString(respuesta).getAsJsonObject();
                },
                respuesta -> {
                    if (respuesta.get("exito").getAsBoolean()) {
                        Platform.runLater(() -> {
                            mostrarAlerta("Éxito", "Contenido eliminado del grupo", Alert.AlertType.INFORMATION);
                            cargarDatos(); // Refrescar lista
                        });
                    } else {
                        Platform.runLater(() ->
                                mostrarAlerta("Error",
                                        respuesta.has("mensaje") ? respuesta.get("mensaje").getAsString() : "Error desconocido",
                                        Alert.AlertType.ERROR)
                        );
                    }
                },
                error -> Platform.runLater(() ->
                        mostrarAlerta("Error", "No se pudo eliminar el contenido: " + error.getMessage(), Alert.AlertType.ERROR)
                ),
                "eliminar contenido del grupo"
        );
    }

    private String obtenerIdContenido(String titulo) {
        if (grupoData.has("contenidos") && !grupoData.get("contenidos").isJsonNull()) {
            for (JsonElement elemento : grupoData.get("contenidos").getAsJsonArray()) {
                if (!elemento.isJsonNull()) {
                    JsonObject contenido = elemento.getAsJsonObject();
                    if (contenido.has("titulo") && !contenido.get("titulo").isJsonNull()
                            && contenido.get("titulo").getAsString().equals(titulo)) {
                        return contenido.has("id") && !contenido.get("id").isJsonNull()
                                ? contenido.get("id").getAsString()
                                : null;
                    }
                }
            }
        }
        return null;
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
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