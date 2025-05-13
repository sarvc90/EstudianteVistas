module com.taller.estudiantevistas {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.taller.estudiantevistas to javafx.fxml;
    opens com.taller.estudiantevistas.controlador to javafx.fxml;
    opens com.taller.estudiantevistas.css to javafx.fxml;
    opens com.taller.estudiantevistas.fxml to javafx.fxml;
    opens com.taller.estudiantevistas.icons to javafx.graphics;

    exports com.taller.estudiantevistas;
    exports com.taller.estudiantevistas.controlador;
}