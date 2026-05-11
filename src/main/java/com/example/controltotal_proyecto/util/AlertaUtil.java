package com.example.controltotal_proyecto.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Muestra alertas de advertencia con estilo personalizado (fondo azul oscuro,
 * icono amarillo de advertencia, texto blanco, botón "Aceptar" oscuro).
 */
public class AlertaUtil {

    /**
     * Muestra una advertencia estilizada modal.
     *
     * @param mensaje texto del mensaje
     * @param owner   ventana propietaria (puede ser null)
     */
    public static void mostrarAdvertencia(String mensaje, Window owner) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Advertencia");
        stage.setResizable(false);

        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }

        // ── Icono ────────────────────────────────────────────────────────────
        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size: 64px; -fx-text-fill: #f5c842;");
        icon.setAlignment(Pos.CENTER);

        // ── Mensaje ──────────────────────────────────────────────────────────
        Label msg = new Label(mensaje);
        msg.setStyle(
            "-fx-text-fill: white; -fx-font-size: 15px; " +
            "-fx-font-family: 'DM Sans','Segoe UI',Arial,sans-serif;"
        );
        msg.setWrapText(true);
        msg.setTextAlignment(TextAlignment.CENTER);
        msg.setMaxWidth(340);
        msg.setAlignment(Pos.CENTER);

        // ── Botón ────────────────────────────────────────────────────────────
        Button btnOk = new Button("Aceptar");
        btnOk.setStyle(
            "-fx-background-color: #111827; -fx-text-fill: white; " +
            "-fx-font-size: 14px; -fx-font-weight: bold; " +
            "-fx-padding: 9 36 9 36; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-border-color: #374151; " +
            "-fx-border-radius: 6; -fx-border-width: 1;"
        );
        btnOk.setOnAction(ev -> stage.close());

        btnOk.setOnMouseEntered(ev -> btnOk.setStyle(
            "-fx-background-color: #1f2937; -fx-text-fill: white; " +
            "-fx-font-size: 14px; -fx-font-weight: bold; " +
            "-fx-padding: 9 36 9 36; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-border-color: #4b5563; " +
            "-fx-border-radius: 6; -fx-border-width: 1;"
        ));
        btnOk.setOnMouseExited(ev -> btnOk.setStyle(
            "-fx-background-color: #111827; -fx-text-fill: white; " +
            "-fx-font-size: 14px; -fx-font-weight: bold; " +
            "-fx-padding: 9 36 9 36; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-border-color: #374151; " +
            "-fx-border-radius: 6; -fx-border-width: 1;"
        ));

        // ── Contenedor raíz ──────────────────────────────────────────────────
        VBox root = new VBox(20, icon, msg, btnOk);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30, 45, 28, 45));
        root.setStyle("-fx-background-color: #2d4a8a;");
        root.setPrefWidth(420);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }
}
