package com.example.controltotal_proyecto;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.InputStream;

public class SplashScreen {

    private final Stage splashStage;
    private Runnable onFinished;

    public SplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setAlwaysOnTop(true);

        StackPane root = new StackPane();
        root.setPrefSize(560, 340);
        root.setStyle(
            "-fx-background-color: #0f1117;" +
            "-fx-border-color: #2a3050;" +
            "-fx-border-width: 1px;"
        );

        VBox content = new VBox(24);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 40 60 40 60;");

        // Logo UNINC cargado por código
        ImageView logoView = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream(
                "/com/example/controltotal_proyecto/images/uninc_logo.png"
            );
            if (is != null) {
                logoView.setImage(new Image(is));
                logoView.setFitWidth(220);
                logoView.setPreserveRatio(true);
                logoView.setSmooth(true);
            }
        } catch (Exception e) {
            System.err.println("Splash: no se pudo cargar el logo: " + e.getMessage());
        }

        // Separador decorativo
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(200);
        sep.setStyle("-fx-background-color: linear-gradient(to right, transparent, #00b894, transparent);");

        Label appTitle = new Label("Control Total");
        appTitle.setStyle(
            "-fx-font-size: 28px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-family: 'Century Gothic', serif;"
        );

        Label subtitle = new Label("Sistema de Gestión de Clientes");
        subtitle.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #8b9bb4;"
        );

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(320);
        progressBar.setPrefHeight(4);
        progressBar.setStyle(
            "-fx-accent: #00b894;" +
            "-fx-background-color: #1e2333;" +
            "-fx-background-radius: 2px;" +
            "-fx-border-radius: 2px;"
        );

        Label statusLabel = new Label("Iniciando...");
        statusLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #8b9bb4;"
        );

        content.getChildren().addAll(logoView, sep, appTitle, subtitle, progressBar, statusLabel);
        root.getChildren().add(content);

        Scene scene = new Scene(root, 560, 340);
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);

        // Animaciones
        content.setOpacity(0);
        content.setTranslateY(20);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(700), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(700), content);
        slideUp.setFromY(20);
        slideUp.setToY(0);
        slideUp.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition intro = new ParallelTransition(fadeIn, slideUp);

        Timeline progress = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(progressBar.progressProperty(), 0)),
            new KeyFrame(Duration.millis(400),
                e -> statusLabel.setText("Conectando a la base de datos..."),
                new KeyValue(progressBar.progressProperty(), 0.35, Interpolator.EASE_IN)),
            new KeyFrame(Duration.millis(900),
                e -> statusLabel.setText("Cargando entidades..."),
                new KeyValue(progressBar.progressProperty(), 0.70, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.millis(1400),
                e -> statusLabel.setText("Listo"),
                new KeyValue(progressBar.progressProperty(), 1.0, Interpolator.EASE_OUT))
        );

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.millis(300));
        fadeOut.setOnFinished(e -> {
            splashStage.close();
            if (onFinished != null) onFinished.run();
        });

        intro.setOnFinished(e -> {
            progress.play();
            progress.setOnFinished(pe -> fadeOut.play());
        });

        splashStage.setOnShown(e -> intro.play());
    }

    public void show(Runnable onFinished) {
        this.onFinished = onFinished;
        splashStage.centerOnScreen();
        splashStage.show();
    }
}
