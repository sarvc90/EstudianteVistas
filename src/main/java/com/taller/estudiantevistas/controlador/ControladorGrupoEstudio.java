package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ControladorGrupoEstudio {

    @FXML private Label membersCountLabel;

    @FXML
    private void initialize() {
        // Inicialización si es necesaria
    }

    @FXML private void irAlChat() {
        mostrarAlerta("Chat Grupal", "Redirigiendo al chat grupal...");
    }

    @FXML private void unirseAlGrupo() {
        mostrarAlerta("Unirse al Grupo", "Te has unido al grupo de estudio");
    }

    @FXML private void verMiembros() {
        mostrarAlerta("Miembros", "Lista de miembros del grupo");
    }

    @FXML private void verContenido() {
        mostrarAlerta("Contenido", "Contenido del grupo de estudio");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class ControladorPrincipal {
        @FXML private TextField campoBusqueda;
        @FXML private ComboBox<String> comboTipo;

        @FXML
        public void initialize() {
            comboTipo.getItems().addAll("Todos", "Educativos", "Ayuda");
        }
    }
}