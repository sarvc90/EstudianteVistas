package com.taller.estudiantevistas.controlador;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class ControladorAjustesUsuario {
    @FXML private TextField nombreField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private ClienteServicio cliente;
    private String usuarioId;
    private Stage stage;

    public void inicializar(String usuarioId, ClienteServicio cliente, Stage stage) {
        this.cliente = cliente;
        this.stage = stage;

        // Si usuarioId viene como JSON stringificado, extraemos el id real
        if (usuarioId.trim().startsWith("{")) {
            try {
                JsonObject obj = JsonParser.parseString(usuarioId).getAsJsonObject();
                this.usuarioId = obj.get("id").getAsString();
            } catch (Exception e) {
                // Si falla, dejar el valor original y mostrar error o log
                this.usuarioId = usuarioId;
                System.err.println("Error al parsear usuarioId: " + e.getMessage());
            }
        } else {
            this.usuarioId = usuarioId; // ya es un id simple
        }

        pedirDatosUsuarioDesdeServidor();
    }


    private void pedirDatosUsuarioDesdeServidor() {
        // Ejecutar en hilo aparte para no bloquear UI
        new Thread(() -> {
            try {
                JsonObject solicitud = new JsonObject();
                solicitud.addProperty("id", usuarioId);

                JsonObject solicitudCompleta = new JsonObject();
                solicitudCompleta.addProperty("tipo", "OBTENER_USUARIO");
                solicitudCompleta.add("datos", solicitud);

                // Enviar solicitud al servidor
                cliente.getSalida().println(solicitudCompleta.toString());

                // Leer respuesta
                String respuesta = cliente.getEntrada().readLine();

                JsonObject respuestaJson = JsonParser.parseString(respuesta).getAsJsonObject();

                if (respuestaJson.get("exito").getAsBoolean()) {
                    JsonObject usuarioData = respuestaJson.getAsJsonObject("usuario");

                    // Actualizar UI en hilo de JavaFX
                    Platform.runLater(() -> {
                        nombreField.setText(usuarioData.has("nombre") && !usuarioData.get("nombre").isJsonNull()
                                ? usuarioData.get("nombre").getAsString() : "");
                        emailField.setText(usuarioData.has("email") && !usuarioData.get("email").isJsonNull()
                                ? usuarioData.get("email").getAsString() : "");
                        passwordField.setText(""); // no mostrar contraseña
                    });

                } else {
                    String mensaje = respuestaJson.has("mensaje") ? respuestaJson.get("mensaje").getAsString() : "Error desconocido";
                    Platform.runLater(() -> mostrarAlerta("Error", mensaje, Alert.AlertType.ERROR));
                }

            } catch (IOException e) {
                Platform.runLater(() -> mostrarAlerta("Error", "Error comunicándose con el servidor", Alert.AlertType.ERROR));
            }
        }).start();
    }

    @FXML
    private void actualizarDatos() {
        JsonObject datosActualizados = new JsonObject();
        datosActualizados.addProperty("id", usuarioId);
        datosActualizados.addProperty("nombre", nombreField.getText());
        datosActualizados.addProperty("email", emailField.getText());
        datosActualizados.addProperty("password", passwordField.getText());

        new Thread(() -> {
            JsonObject solicitudCompleta = new JsonObject();
            solicitudCompleta.addProperty("tipo", "ACTUALIZAR_USUARIO");
            solicitudCompleta.add("datos", datosActualizados);

            cliente.getSalida().println(solicitudCompleta.toString());

            try {
                String respuesta = cliente.getEntrada().readLine();
                JsonObject respuestaJson = JsonParser.parseString(respuesta).getAsJsonObject();

                if (respuestaJson.get("exito").getAsBoolean()) {
                    Platform.runLater(() -> mostrarAlerta("Éxito", "Datos actualizados correctamente", Alert.AlertType.INFORMATION));
                } else {
                    String mensaje = respuestaJson.has("mensaje") ? respuestaJson.get("mensaje").getAsString() : "Error desconocido";
                    Platform.runLater(() -> mostrarAlerta("Error", mensaje, Alert.AlertType.ERROR));
                }
            } catch (IOException e) {
                Platform.runLater(() -> mostrarAlerta("Error", "Error comunicándose con el servidor", Alert.AlertType.ERROR));
            }
        }).start();
    }


    @FXML
    private void solicitarEliminarCuenta() {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            mostrarAlerta("Error", "ID de usuario no disponible para eliminar", Alert.AlertType.ERROR);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro de eliminar su cuenta?");
        confirmacion.setContentText("Esta acción no se puede deshacer. Todos sus datos se perderán permanentemente.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            new Thread(() -> {
                JsonObject solicitudCompleta = new JsonObject();
                solicitudCompleta.addProperty("tipo", "ELIMINAR_USUARIO");
                JsonObject datos = new JsonObject();
                datos.addProperty("id", usuarioId);
                solicitudCompleta.add("datos", datos);

                System.out.println("Solicitud eliminar a enviar: " + solicitudCompleta.toString());

                cliente.getSalida().println(solicitudCompleta.toString());

                try {
                    String respuesta = cliente.getEntrada().readLine();
                    System.out.println("Respuesta del servidor: " + respuesta);

                    JsonObject respuestaJson = JsonParser.parseString(respuesta).getAsJsonObject();

                    if (respuestaJson.get("exito").getAsBoolean()) {
                        Platform.runLater(() -> {
                            mostrarAlerta("Éxito", "Cuenta eliminada correctamente", Alert.AlertType.INFORMATION);

                            try {
                                URL fxmlUrl = getClass().getResource("/com/taller/estudiantevistas/fxml/login.fxml");
                                if (fxmlUrl == null) {
                                    mostrarAlerta("Error", "No se encontró el archivo FXML de login", Alert.AlertType.ERROR);
                                    return;
                                }

                                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                                Parent root = loader.load();

                                Stage loginStage = new Stage();
                                loginStage.setTitle("Login");
                                loginStage.setScene(new Scene(root));
                                loginStage.show();

                                stage.close();

                            } catch (IOException e) {
                                mostrarAlerta("Error", "No se pudo abrir la ventana de login", Alert.AlertType.ERROR);
                            }
                        });
                    } else {
                        String mensaje = respuestaJson.has("mensaje") ? respuestaJson.get("mensaje").getAsString() : "Error desconocido";
                        Platform.runLater(() -> mostrarAlerta("Error", mensaje, Alert.AlertType.ERROR));
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> mostrarAlerta("Error", "Error comunicándose con el servidor", Alert.AlertType.ERROR));
                }
            }).start();
        }
    }




    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
