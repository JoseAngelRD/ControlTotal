package com.example.controltotal_proyecto;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Punto de entrada de la aplicación Control Total — UNINC.
 * La ventana se abre al 75 % de la pantalla del monitor actual,
 * centrada, y es libremente redimensionable por el usuario.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/example/controltotal_proyecto/fxml/MainLayout.fxml")
        );

        // ── Tamaño inicial: 80 % de la pantalla disponible ───────────────────
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double initW = Math.round(screen.getWidth()  * 0.80);
        double initH = Math.round(screen.getHeight() * 0.80);

        Scene scene = new Scene(loader.load(), initW, initH);
        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("/com/example/controltotal_proyecto/css/styles.css")
            ).toExternalForm()
        );

        primaryStage.setTitle("Control Total — UNINC Asesores Legales");
        primaryStage.setMinWidth(860);
        primaryStage.setMinHeight(580);
        primaryStage.setResizable(true);   // el usuario puede redimensionar libremente
        primaryStage.setScene(scene);

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
