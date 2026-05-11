package com.example.controltotal_proyecto;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Punto de entrada de la aplicación Control Total — UNINC.
 * Muestra el splash screen y luego carga la ventana principal.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ── Preparar la ventana principal (sin mostrarla aún) ─────────────────
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/example/controltotal_proyecto/fxml/MainLayout.fxml")
        );

        Scene scene = new Scene(loader.load(), 1680, 950);
        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("/com/example/controltotal_proyecto/css/styles.css")
            ).toExternalForm()
        );

        primaryStage.setTitle("Control Total — UNINC Asesores Legales");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.setScene(scene);
        // NO llamamos primaryStage.show() todavía

        // ── Mostrar splash y al terminar abrir la app ─────────────────────────
        SplashScreen splash = new SplashScreen();
        splash.show(() -> Platform.runLater(() -> {
            primaryStage.centerOnScreen();
            primaryStage.show();
        }));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
