package com.taller.estudiantevistas.controlador;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ControladorGestionUsuarios {

    @FXML private TableView<UsuarioTabla> tablaUsuarios;
    @FXML private TableColumn<UsuarioTabla, String> colNombre, colCorreo, colEstado;
    @FXML private TableColumn<UsuarioTabla, LocalDate> colSuspension;
    @FXML private Button btnSuspender, btnEliminar, btnReactivar;

    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private String moderadorId;

    public void inicializar(String moderadorId) {
        System.out.println("[DEBUG] Inicializando ControladorGestionUsuarios");
        System.out.println("[DEBUG] ID Moderador: " + moderadorId);

        this.moderadorId = moderadorId;
        conectarAlServidor();
        configurarTabla();
        cargarUsuarios();
    }

    private void conectarAlServidor() {
        try {
            String host = "localhost";
            int puerto = 1234;

            System.out.println("[DEBUG] Conectando al servidor en " + host + ":" + puerto);
            this.socket = new Socket(host, puerto);
            this.salida = new PrintWriter(socket.getOutputStream(), true);
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("[DEBUG] Conexión establecida exitosamente");
        } catch (Exception e) {
            System.err.println("[ERROR] Error al conectar al servidor: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo conectar al servidor", Alert.AlertType.ERROR);
        }
    }

    private void configurarTabla() {
        System.out.println("[DEBUG] Configurando columnas de la tabla");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colSuspension.setCellValueFactory(new PropertyValueFactory<>("fechaSuspension"));

        System.out.println("[DEBUG] Configurando listener de selección de tabla");
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("[DEBUG] Usuario seleccionado: " + (newVal != null ? newVal.getNombre() : "ninguno"));
            boolean seleccionado = newVal != null;
            btnSuspender.setDisable(!seleccionado);
            btnEliminar.setDisable(!seleccionado);
            btnReactivar.setDisable(!seleccionado || (newVal != null && newVal.getEstado().equals("ACTIVO")));
        });
    }

    @FXML
    private void cargarUsuarios() {
        System.out.println("[DEBUG] Solicitando lista de usuarios al servidor");
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "OBTENER_TODOS_USUARIOS");

                        System.out.println("[DEBUG] Enviando solicitud: " + solicitud);
                        salida.println(solicitud.toString());

                        System.out.println("[DEBUG] Esperando respuesta...");
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            System.err.println("[ERROR] El servidor no respondió");
                            throw new IOException("El servidor no respondió");
                        }

                        System.out.println("[DEBUG] Respuesta recibida: " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        System.err.println("[ERROR] Error de comunicación: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            System.out.println("[DEBUG] Carga de usuarios exitosa");
                            JsonArray usuarios = jsonRespuesta.getAsJsonArray("usuarios");
                            System.out.println("[DEBUG] Usuarios recibidos: " + usuarios.size());
                            Platform.runLater(() -> actualizarTabla(usuarios));
                        } else {
                            String mensajeError = jsonRespuesta.get("mensaje").getAsString();
                            System.err.println("[ERROR] Error al cargar usuarios: " + mensajeError);
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR)
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error al procesar respuesta: " + e.getMessage());
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "Error al procesar respuesta del servidor", Alert.AlertType.ERROR)
                        );
                    }
                },
                "carga de usuarios"
        );
    }

    private void actualizarTabla(JsonArray usuarios) {
        System.out.println("[DEBUG] Actualizando tabla con " + usuarios.size() + " usuarios");
        ObservableList<UsuarioTabla> usuariosList = FXCollections.observableArrayList();

        usuarios.forEach(usuario -> {
            JsonObject user = usuario.getAsJsonObject();
            String nombre = user.get("nombre").getAsString();
            String correo = user.get("correo").getAsString();
            String estado = user.get("estado").getAsString();
            LocalDate fechaSuspension = user.has("fechaSuspension") && !user.get("fechaSuspension").isJsonNull() ?
                    LocalDate.parse(user.get("fechaSuspension").getAsString()) : null;

            System.out.println("[DEBUG] Agregando usuario: " + nombre + " (" + correo + ") - Estado: " + estado);

            usuariosList.add(new UsuarioTabla(
                    nombre,
                    correo,
                    estado,
                    fechaSuspension
            ));
        });

        tablaUsuarios.setItems(usuariosList);
        System.out.println("[DEBUG] Tabla actualizada exitosamente");
    }

    @FXML
    private void manejarSuspender() {
        UsuarioTabla usuario = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (usuario == null) {
            System.out.println("[DEBUG] Intento de suspender usuario sin selección");
            return;
        }

        System.out.println("[DEBUG] Iniciando suspensión para usuario: " + usuario.getNombre());
        TextInputDialog dialog = new TextInputDialog("7");
        dialog.setTitle("Suspender Usuario");
        dialog.setHeaderText("Suspender a " + usuario.getNombre());
        dialog.setContentText("Días de suspensión:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(dias -> {
            try {
                int diasSuspension = Integer.parseInt(dias);
                System.out.println("[DEBUG] Días de suspensión ingresados: " + diasSuspension);
                suspenderUsuario(usuario.getCorreo(), diasSuspension);
            } catch (NumberFormatException e) {
                System.err.println("[ERROR] Días de suspensión inválidos: " + dias);
                mostrarAlerta("Error", "Debe ingresar un número válido", Alert.AlertType.ERROR);
            }
        });
    }

    private void suspenderUsuario(String correo, int dias) {
        System.out.println("[DEBUG] Enviando solicitud de suspensión para " + correo + " por " + dias + " días");
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "SUSPENDER_USUARIO");

                        JsonObject datos = new JsonObject();
                        datos.addProperty("correoUsuario", correo);
                        datos.addProperty("diasSuspension", dias);
                        datos.addProperty("moderadorId", moderadorId);
                        solicitud.add("datos", datos);

                        System.out.println("[DEBUG] Enviando solicitud: " + solicitud);
                        salida.println(solicitud.toString());

                        System.out.println("[DEBUG] Esperando respuesta...");
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            System.err.println("[ERROR] El servidor no respondió");
                            throw new IOException("El servidor no respondió");
                        }

                        System.out.println("[DEBUG] Respuesta recibida: " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        System.err.println("[ERROR] Error de comunicación: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            System.out.println("[DEBUG] Usuario suspendido exitosamente");
                            Platform.runLater(() -> {
                                mostrarAlerta("Éxito", "Usuario suspendido correctamente", Alert.AlertType.INFORMATION);
                                cargarUsuarios();
                            });
                        } else {
                            String mensajeError = jsonRespuesta.get("mensaje").getAsString();
                            System.err.println("[ERROR] Error al suspender usuario: " + mensajeError);
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR)
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error al procesar respuesta: " + e.getMessage());
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "Error al procesar respuesta del servidor", Alert.AlertType.ERROR)
                        );
                    }
                },
                "suspensión de usuario"
        );
    }

    @FXML
    private void manejarEliminar() {
        UsuarioTabla usuario = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (usuario == null) {
            System.out.println("[DEBUG] Intento de eliminar usuario sin selección");
            return;
        }

        System.out.println("[DEBUG] Iniciando eliminación para usuario: " + usuario.getNombre());
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Eliminación");
        confirmacion.setHeaderText("Eliminar usuario " + usuario.getNombre());
        confirmacion.setContentText("¿Está seguro de eliminar este usuario permanentemente?");

        Optional<ButtonType> result = confirmacion.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("[DEBUG] Confirmada eliminación de usuario");
            eliminarUsuario(usuario.getCorreo());
        } else {
            System.out.println("[DEBUG] Eliminación cancelada por el usuario");
        }
    }

    private void eliminarUsuario(String correo) {
        System.out.println("[DEBUG] Enviando solicitud de eliminación para " + correo);
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "ELIMINAR_USUARIO");

                        JsonObject datos = new JsonObject();
                        datos.addProperty("correoUsuario", correo);
                        datos.addProperty("moderadorId", moderadorId);
                        solicitud.add("datos", datos);

                        System.out.println("[DEBUG] Enviando solicitud: " + solicitud);
                        salida.println(solicitud.toString());

                        System.out.println("[DEBUG] Esperando respuesta...");
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            System.err.println("[ERROR] El servidor no respondió");
                            throw new IOException("El servidor no respondió");
                        }

                        System.out.println("[DEBUG] Respuesta recibida: " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        System.err.println("[ERROR] Error de comunicación: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            System.out.println("[DEBUG] Usuario eliminado exitosamente");
                            Platform.runLater(() -> {
                                mostrarAlerta("Éxito", "Usuario eliminado correctamente", Alert.AlertType.INFORMATION);
                                cargarUsuarios();
                            });
                        } else {
                            String mensajeError = jsonRespuesta.get("mensaje").getAsString();
                            System.err.println("[ERROR] Error al eliminar usuario: " + mensajeError);
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR)
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error al procesar respuesta: " + e.getMessage());
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "Error al procesar respuesta del servidor", Alert.AlertType.ERROR)
                        );
                    }
                },
                "eliminación de usuario"
        );
    }

    @FXML
    private void manejarReactivar() {
        UsuarioTabla usuario = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (usuario == null) {
            System.out.println("[DEBUG] Intento de reactivar usuario sin selección");
            return;
        }

        System.out.println("[DEBUG] Iniciando reactivación para usuario: " + usuario.getNombre());
        reactivarUsuario(usuario.getCorreo());
    }

    private void reactivarUsuario(String correo) {
        System.out.println("[DEBUG] Enviando solicitud de reactivación para " + correo);
        ejecutarTareaAsync(
                () -> {
                    try {
                        JsonObject solicitud = new JsonObject();
                        solicitud.addProperty("tipo", "REACTIVAR_USUARIO");

                        JsonObject datos = new JsonObject();
                        datos.addProperty("correoUsuario", correo);
                        datos.addProperty("moderadorId", moderadorId);
                        solicitud.add("datos", datos);

                        System.out.println("[DEBUG] Enviando solicitud: " + solicitud);
                        salida.println(solicitud.toString());

                        System.out.println("[DEBUG] Esperando respuesta...");
                        String respuesta = entrada.readLine();
                        if (respuesta == null) {
                            System.err.println("[ERROR] El servidor no respondió");
                            throw new IOException("El servidor no respondió");
                        }

                        System.out.println("[DEBUG] Respuesta recibida: " + respuesta);
                        return respuesta;
                    } catch (IOException e) {
                        System.err.println("[ERROR] Error de comunicación: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },
                respuesta -> {
                    try {
                        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();
                        if (jsonRespuesta.get("exito").getAsBoolean()) {
                            System.out.println("[DEBUG] Usuario reactivado exitosamente");
                            Platform.runLater(() -> {
                                mostrarAlerta("Éxito", "Usuario reactivado correctamente", Alert.AlertType.INFORMATION);
                                cargarUsuarios();
                            });
                        } else {
                            String mensajeError = jsonRespuesta.get("mensaje").getAsString();
                            System.err.println("[ERROR] Error al reactivar usuario: " + mensajeError);
                            Platform.runLater(() ->
                                    mostrarAlerta("Error", mensajeError, Alert.AlertType.ERROR)
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error al procesar respuesta: " + e.getMessage());
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "Error al procesar respuesta del servidor", Alert.AlertType.ERROR)
                        );
                    }
                },
                "reactivación de usuario"
        );
    }

    private <T> void ejecutarTareaAsync(Supplier<T> tarea, Consumer<T> onSuccess, String contexto) {
        System.out.println("[DEBUG] Iniciando tarea asíncrona: " + contexto);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                System.out.println("[DEBUG] Ejecutando tarea en segundo plano");
                return tarea.get();
            }
        };

        task.setOnSucceeded(e -> {
            System.out.println("[DEBUG] Tarea completada exitosamente");
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            System.err.println("[ERROR] Error en tarea asíncrona: " + task.getException().getMessage());
            Platform.runLater(() ->
                    mostrarAlerta("Error", "Error en " + contexto + ": " + task.getException().getMessage(), Alert.AlertType.ERROR)
            );
        });

        executor.execute(task);
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        System.out.println("[DEBUG] Mostrando alerta: " + titulo + " - " + mensaje);
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class UsuarioTabla {
        private final String nombre;
        private final String correo;
        private String estado;
        private LocalDate fechaSuspension;

        public UsuarioTabla(String nombre, String correo, String estado, LocalDate fechaSuspension) {
            this.nombre = nombre;
            this.correo = correo;
            this.estado = estado;
            this.fechaSuspension = fechaSuspension;
        }

        public String getNombre() { return nombre; }
        public String getCorreo() { return correo; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public LocalDate getFechaSuspension() { return fechaSuspension; }
        public void setFechaSuspension(LocalDate fechaSuspension) { this.fechaSuspension = fechaSuspension; }
    }
}