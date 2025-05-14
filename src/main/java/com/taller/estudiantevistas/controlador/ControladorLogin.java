package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class ControladorLogin {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345; // Asegúrate que coincide con el servidor

    private ClienteServicio cliente;

    @FXML
    private TextField nombreField;
    @FXML
    private PasswordField contrasenaField;
    @FXML
    private Label registerLink;

    @FXML
    public void initialize() {
        try {
            // Establecer conexión persistente al cargar la vista
            this.cliente = new ClienteServicio(SERVER_HOST, SERVER_PORT);
            System.out.println("Conexión establecida con el servidor");
        } catch (IOException e) {
            mostrarAlerta("Error crítico",
                    "No se pudo conectar al servidor:\n" + e.getMessage(),
                    AlertType.ERROR);
            // Deshabilitar interfaz si no hay conexión
            nombreField.setDisable(true);
            contrasenaField.setDisable(true);
        }
    }

    @FXML
    public void iniciarSesion() {
        if (cliente == null || !cliente.estaConectado()) {
            mostrarAlerta("Error", "No hay conexión con el servidor", AlertType.ERROR);
            return;
        }

        String correo = nombreField.getText().trim();
        String contrasena = contrasenaField.getText().trim();

        if (correo.isEmpty() || contrasena.isEmpty()) {
            mostrarAlerta("Error", "Todos los campos son obligatorios", AlertType.ERROR);
            return;
        }

        try {
            JsonObject loginRequest = new JsonObject();
            loginRequest.addProperty("tipo", "LOGIN");

            JsonObject datos = new JsonObject();
            datos.addProperty("correo", correo);
            datos.addProperty("contrasena", contrasena);
            loginRequest.add("datos", datos);

            // Usar la conexión persistente
            cliente.getSalida().println(loginRequest.toString());
            String respuesta = cliente.getEntrada().readLine();

            if (respuesta == null) {
                mostrarAlerta("Error", "El servidor no respondió", AlertType.ERROR);
                return;
            }

            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

            if (jsonRespuesta.get("exito").getAsBoolean()) {
                abrirPantallaPrincipal(jsonRespuesta.get("usuario").toString());
            } else {
                String mensaje = jsonRespuesta.has("mensaje")
                        ? jsonRespuesta.get("mensaje").getAsString()
                        : "Error desconocido";
                mostrarAlerta("Error", mensaje, AlertType.ERROR);
            }
        } catch (IOException e) {
            mostrarAlerta("Error", "Fallo en la comunicación: " + e.getMessage(), AlertType.ERROR);
            cliente.cerrarConexion();
        }
    }

    private void abrirPantallaPrincipal(String datosUsuario) {
        try {
            Stage stage = (Stage) nombreField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/principal.fxml"));
            Parent root = loader.load();

            ControladorPrincipal controlador = loader.getController();
            controlador.inicializarConUsuario(datosUsuario, this.cliente); // Pasar ambos parámetros

            Stage principalStage = new Stage();
            principalStage.setScene(new Scene(root));
            principalStage.setTitle("Red Social Educativa");
            principalStage.show();

            stage.close();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo cargar la pantalla principal", AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    public void registrarse(MouseEvent event) {
        try {
            Stage stage = (Stage) nombreField.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/registro.fxml"));
            Parent root = loader.load();

            Stage registroStage = new Stage();
            registroStage.setScene(new Scene(root));
            registroStage.setTitle("Registro - Red Social Educativa");
            registroStage.show();

            stage.close();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo abrir la ventana de registro", AlertType.ERROR);
            e.printStackTrace();
        }
    }
}