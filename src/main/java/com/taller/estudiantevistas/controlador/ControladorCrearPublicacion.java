package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taller.estudiantevistas.dto.TipoContenido;
import com.taller.estudiantevistas.servicio.ClienteServicio;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ControladorCrearPublicacion {

    @FXML private TextField tituloField;
    @FXML private TextField descripcionField;
    @FXML private TextField etiquetasField;
    @FXML private TextField enlaceField;
    @FXML private Button agregarArchivoBtn;

    private JsonObject usuarioData;
    private ClienteServicio cliente;
    private File archivoSeleccionado;

    // Extensiones para cada tipo de contenido
// Extensiones actualizadas para coincidir con los tipos del servidor
    private static final List<String> EXTENSIONES_DOCUMENTO = Arrays.asList(
            ".pdf", ".doc", ".docx", ".txt", ".odt", ".rtf"
    );

    private static final List<String> EXTENSIONES_VIDEO = Arrays.asList(
            ".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv"
    );

    private static final List<String> EXTENSIONES_PRESENTACION = Arrays.asList(
            ".ppt", ".pptx", ".odp", ".key"
    );

    @FXML
    public void initialize() {
        agregarArchivoBtn.setOnAction(event -> agregarArchivo());
        enlaceField.setEditable(false); // El enlace se llenará automáticamente
    }

    public void inicializar(JsonObject usuarioData, ClienteServicio cliente) {
        this.usuarioData = usuarioData;
        this.cliente = cliente;
    }

    @FXML
    private void publicar() {
        if (!validarCampos()) return;

        try {
            TipoContenido tipo = determinarTipoContenido();

            JsonObject publicacion = new JsonObject();
            publicacion.addProperty("tipo", "CREAR_PUBLICACION");

            JsonObject datos = new JsonObject();
            datos.addProperty("usuarioId", usuarioData.get("id").getAsString());
            datos.addProperty("titulo", tituloField.getText());
            datos.addProperty("descripcion", descripcionField.getText());
            datos.addProperty("tema", etiquetasField.getText());
            datos.addProperty("tipoContenido", tipo.name());
            datos.addProperty("contenido",
                    archivoSeleccionado != null ?
                            archivoSeleccionado.getAbsolutePath() :
                            enlaceField.getText()
            );

            publicacion.add("datos", datos);

            // Enviar y esperar respuesta
            cliente.getSalida().println(publicacion.toString());
            String respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

            if (jsonRespuesta.get("exito").getAsBoolean()) {
                mostrarAlerta("Éxito", "Publicación creada", Alert.AlertType.INFORMATION);
                limpiarCampos();

                // Cerrar la ventana actual
                Stage stageActual = (Stage) tituloField.getScene().getWindow();
                stageActual.close();

                // Abrir la ventana de perfil
                abrirVentanaPerfil();
            } else {
                mostrarAlerta("Error", jsonRespuesta.get("mensaje").getAsString(), Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            mostrarAlerta("Error", "Error al publicar: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void abrirVentanaPerfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taller/estudiantevistas/fxml/perfil.fxml"));
            Parent root = loader.load();

            // Obtener el controlador del perfil y pasarle los datos necesarios
            ControladorPerfil controladorPerfil = loader.getController();
            controladorPerfil.inicializar(usuarioData, cliente);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Perfil de Usuario");
            stage.show();

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la ventana de perfil", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    @FXML
    private void agregarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo");

        // Configurar filtros de extensión
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov", "*.mkv"),
                new FileChooser.ExtensionFilter("Presentaciones", "*.ppt", "*.pptx", "*.odp"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        archivoSeleccionado = fileChooser.showOpenDialog(tituloField.getScene().getWindow());

        if (archivoSeleccionado != null) {
            // Mostrar la ruta del archivo en el campo de enlace
            enlaceField.setText(archivoSeleccionado.getAbsolutePath());
            mostrarAlerta("Archivo", "Archivo seleccionado: " + archivoSeleccionado.getName(), Alert.AlertType.INFORMATION);
        }
    }

    private TipoContenido determinarTipoContenido() {
        if (archivoSeleccionado == null) {
            return TipoContenido.ENLACE;
        }

        String nombreArchivo = archivoSeleccionado.getName().toLowerCase();

        // Verificar extensiones para DOCUMENTO (PDF, Word, TXT)
        if (EXTENSIONES_DOCUMENTO.stream().anyMatch(nombreArchivo::endsWith)) {
            return TipoContenido.DOCUMENTO;
        }

        // Verificar extensiones para VIDEO
        if (EXTENSIONES_VIDEO.stream().anyMatch(nombreArchivo::endsWith)) {
            return TipoContenido.VIDEO;
        }

        // Verificar extensiones para PRESENTACION
        if (EXTENSIONES_PRESENTACION.stream().anyMatch(nombreArchivo::endsWith)) {
            return TipoContenido.PRESENTACION;
        }

        return TipoContenido.OTRO; // Para cualquier otra extensión no reconocida
    }

    private boolean validarCampos() {
        if (tituloField.getText().isEmpty()) {
            mostrarAlerta("Error", "El título es obligatorio", Alert.AlertType.ERROR);
            return false;
        }

        if (archivoSeleccionado == null && enlaceField.getText().isEmpty()) {
            mostrarAlerta("Error", "Debe agregar un archivo o un enlace", Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    private void limpiarCampos() {
        tituloField.clear();
        descripcionField.clear();
        etiquetasField.clear();
        enlaceField.clear();
        archivoSeleccionado = null;
    }

    private void cerrarVentana() {
        Stage stage = (Stage) tituloField.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }


}