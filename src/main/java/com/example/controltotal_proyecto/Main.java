package com.example.controltotal_proyecto;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Punto de entrada de la aplicación AuditGest.
 * Carga el layout principal y aplica la hoja de estilos global.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/example/controltotal_proyecto/fxml/MainLayout.fxml")
        );

        Scene scene = new Scene(loader.load(), 1680, 950);
        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("/com/example/controltotal_proyecto/css/styles.css")
            ).toExternalForm()
        );

        primaryStage.setTitle("Control Total");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
