package com.taller.estudiantevistas.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class ControladorBuscar {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private VBox resultsContainer;

    @FXML
    private void initialize() {
        // Cargar fuente Roboto Mono
        Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoMono-Regular.ttf"), 12);

        // Configurar acción al presionar Enter
        searchField.setOnAction(event -> handleSearch());
    }

    @FXML private Label titleLabel;

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        resultsContainer.getChildren().clear();
        resultsContainer.getChildren().add(titleLabel); // Mantener el título

        if (!query.isEmpty()) {
            Label result = new Label("Resultado para: " + query);
            result.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px;");
            resultsContainer.getChildren().add(result);
        }
    }

}