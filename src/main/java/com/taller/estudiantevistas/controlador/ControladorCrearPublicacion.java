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
        enlaceField.setEditable(false);
    }

    public void inicializar(JsonObject usuarioData, ClienteServicio cliente) {
        this.usuarioData = usuarioData;
        this.cliente = cliente;
    }

    @FXML
    private void publicar() {
        if (!validarCampos()) return;

        try {
            // 1. Determinar tipo de contenido
            TipoContenido tipo = determinarTipoContenido();

            // 2. Obtener la ruta del contenido (archivo o enlace)
            String contenidoRuta = archivoSeleccionado != null ?
                    archivoSeleccionado.getAbsolutePath() :
                    enlaceField.getText();

            // 3. Construir el JSON de solicitud
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "CREAR_PUBLICACION");

            JsonObject datos = new JsonObject();
            datos.addProperty("usuarioId", usuarioData.get("id").getAsString());
            datos.addProperty("titulo", tituloField.getText().trim());
            datos.addProperty("descripcion", descripcionField.getText().trim());
            datos.addProperty("tema", etiquetasField.getText().trim());
            datos.addProperty("tipoContenido", tipo.name());
            datos.addProperty("contenido", contenidoRuta);

            solicitud.add("datos", datos);

            // 4. Enviar solicitud usando los métodos públicos existentes
            cliente.getSalida().println(solicitud.toString());
            String respuesta = cliente.getEntrada().readLine();
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

            // 5. Procesar respuesta
            if (jsonRespuesta.get("exito").getAsBoolean()) {
                mostrarAlerta("Éxito", "Publicación creada exitosamente", Alert.AlertType.INFORMATION);
                cerrarVentana();
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

            ControladorPerfil controlador = loader.getController();
            controlador.inicializar(usuarioData, cliente);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Perfil de Usuario");
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir el perfil", Alert.AlertType.ERROR);
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
            enlaceField.setText(archivoSeleccionado.getAbsolutePath());
        }
    }

    private TipoContenido determinarTipoContenido() {
        if (archivoSeleccionado == null) {
            return TipoContenido.ENLACE;
        }

        String nombreArchivo = archivoSeleccionado.getName().toLowerCase();

        if (EXTENSIONES_DOCUMENTO.stream().anyMatch(nombreArchivo::endsWith)) {
            return TipoContenido.DOCUMENTO;
        }
        if (EXTENSIONES_VIDEO.stream().anyMatch(nombreArchivo::endsWith)) {
            return TipoContenido.VIDEO;
        }
        if (EXTENSIONES_PRESENTACION.stream().anyMatch(nombreArchivo::endsWith)) {
            return TipoContenido.PRESENTACION;
        }

        return TipoContenido.OTRO;
    }

    private boolean validarCampos() {
        if (tituloField.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El título es obligatorio", Alert.AlertType.ERROR);
            return false;
        }
        if (archivoSeleccionado == null && enlaceField.getText().trim().isEmpty()) {
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