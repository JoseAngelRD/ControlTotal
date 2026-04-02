module com.example.controltotal_proyecto {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.controltotal_proyecto to javafx.fxml;
    exports com.example.controltotal_proyecto;
    exports com.example.controltotal_proyecto.controller;
    opens com.example.controltotal_proyecto.controller to javafx.fxml;
    opens com.example.controltotal_proyecto.entities to javafx.base;
}