package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Controlador para la vista de configuración de un grupo de estudio.
 * Permite a los usuarios editar el nombre y la descripción del grupo,
 * así como eliminar contenidos del mismo.
 */

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
    /**
     * Executor para manejar tareas asíncronas.
     * Utiliza un pool de hilos con hilos daemon para evitar bloquear la aplicación.
     */
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    /**
     * Inicializa el controlador con los datos del grupo, la salida y la entrada.
     * @param grupoData Datos del grupo en formato JSON.
     * @param salida PrintWriter para enviar datos al servidor.
     * @param entrada BufferedReader para recibir datos del servidor.
     */

    public void inicializar(JsonObject grupoData, PrintWriter salida, BufferedReader entrada) {
        this.grupoData = grupoData;
        this.salida = salida;
        this.entrada = entrada;
        cargarDatos();
    }

    /**
     * Carga los datos del grupo en los campos de la interfaz.
     * Verifica que los campos no sean nulos antes de asignar valores.
     */

    private void cargarDatos() {
        // Cargar nombre con verificación de nulidad
        if (grupoData.has("nombre") && !grupoData.get("nombre").isJsonNull()) {
            String nombre = grupoData.get("nombre").getAsString();
            nombreGrupo.setText(nombre);
            txtNombreGrupo.setText(nombre);
        }

        if (grupoData.has("descripcion") && !grupoData.get("descripcion").isJsonNull()) {
            txtDescripcionGrupo.setText(grupoData.get("descripcion").getAsString());
        } else {
            txtDescripcionGrupo.setText("");
        }

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

    /**
     * Maneja el evento de guardar cambios en el grupo.
     * Valida los campos y envía una solicitud al servidor para actualizar la información del grupo.
     */

    @FXML
    private void guardarCambios() {
        String nuevoNombre = txtNombreGrupo.getText().trim();
        String nuevaDescripcion = txtDescripcionGrupo.getText().trim();

        if (nuevoNombre.isEmpty()) {
            mostrarAlerta("Error", "El nombre del grupo no puede estar vacío", Alert.AlertType.ERROR);
            return;
        }

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

    /**
     * Maneja el evento de eliminar un contenido del grupo.
     * Verifica que se haya seleccionado un contenido y envía una solicitud al servidor para eliminarlo.
     */

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
                            cargarDatos();
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

    /**
     * Obtiene el ID del contenido basado en su título.
     * Busca en los contenidos del grupo y devuelve el ID correspondiente.
     * @param titulo Título del contenido a buscar.
     * @return ID del contenido o null si no se encuentra.
     */

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

    /**
     * Muestra una alerta con el título y mensaje proporcionados.
     * @param titulo Título de la alerta.
     * @param mensaje Mensaje de la alerta.
     * @param tipo Tipo de alerta (INFORMATION, ERROR, etc.).
     */

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Ejecuta una tarea asíncrona y maneja el resultado o error.
     * Utiliza un Executor para ejecutar la tarea en un hilo separado.
     * @param tarea Tarea a ejecutar, debe devolver un valor de tipo T.
     * @param onSuccess Consumidor que maneja el resultado exitoso de la tarea.
     * @param onError Consumidor que maneja cualquier error ocurrido durante la tarea.
     * @param contexto Contexto de la tarea, usado para mensajes de error.
     * @param <T> Tipo del resultado de la tarea.
     */

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