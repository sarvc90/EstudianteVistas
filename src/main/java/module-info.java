module com.taller.estudiantevistas {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson; // Para serialización JSON
    requires java.sql; // Si usarás base de datos
    requires org.slf4j; // Para logging (opcional pero recomendado)

    opens com.taller.estudiantevistas to javafx.fxml;
    opens com.taller.estudiantevistas.controlador to javafx.fxml;
    opens com.taller.estudiantevistas.dto to com.google.gson; // Para los DTOs

    exports com.taller.estudiantevistas;
    exports com.taller.estudiantevistas.controlador;
    exports com.taller.estudiantevistas.dto;
    exports com.taller.estudiantevistas.servicio;
}