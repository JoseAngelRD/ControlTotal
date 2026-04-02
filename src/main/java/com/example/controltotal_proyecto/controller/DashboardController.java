package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.*;

/**
 * Controlador del Panel de Control (Dashboard).
 * Muestra KPIs, barras por servicio/delegación y listas recientes.
 */
public class DashboardController implements Initializable,
    MainController.RefreshableController, MainController.ChildController {

    // ─── KPI cards ───────────────────────────────────────────────────────────
    @FXML private Label lblTotalEmpresas;
    @FXML private Label lblActivasEmpresas;
    @FXML private Label lblTotalPersonas;
    @FXML private Label lblRelaciones;

    // ─── Barra de servicios ───────────────────────────────────────────────────
    @FXML private VBox vboxServiciosBars;

    // ─── Barra de delegaciones ────────────────────────────────────────────────
    @FXML private VBox vboxDelegBars;

    // ─── Listas ───────────────────────────────────────────────────────────────
    @FXML private VBox vboxRecientes;
    @FXML private VBox vboxTopPersonas;

    private MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refrescar();
    }

    @Override public void onViewActivated()                      { refrescar(); }
    @Override public void setMainController(MainController main) { this.mainController = main; }

    // ─── Refrescar todos los datos ────────────────────────────────────────────

    private void refrescar() {
        List<Empresa> emps  = DatabaseManager.obtenerTodasLasEmpresas();
        List<Persona> pers  = DatabaseManager.obtenerTodasLasPersonas();
        long activas = emps.stream().filter(Empresa::isActivo).count();
        long rels = pers.stream()
            .mapToLong(p -> DatabaseManager.obtenerEmpresasDePersona(p.getNif()).size())
            .sum();

        // KPIs
        lblTotalEmpresas.setText(String.valueOf(emps.size()));
        lblActivasEmpresas.setText(String.valueOf(activas));
        lblTotalPersonas.setText(String.valueOf(pers.size()));
        lblRelaciones.setText(String.valueOf(rels));

        // Barras por servicio
        List<String> servicios = List.of("Asesoría","Auditoría","Concursal","Pericial");
        List<String> colorsSrv = List.of("#3ecf8e","#c9a84c","#f5a623","#a78bfa");
        renderBars(vboxServiciosBars, servicios, colorsSrv,
            s -> (int) emps.stream().filter(e -> s.equals(e.getServicio())).count(),
            emps.isEmpty() ? 1 : emps.size());

        // Barras por delegación
        List<String> delegs   = List.of("Huelva","Lepe","Puebla de Guzmán");
        List<String> colorsD  = List.of("#4f7dff","#f06060","#3ecf8e");
        renderBars(vboxDelegBars, delegs, colorsD,
            d -> (int) emps.stream().filter(e -> d.equals(e.getDelegacion())).count(),
            emps.isEmpty() ? 1 : emps.size());

        // Empresas recientes
        vboxRecientes.getChildren().clear();
        emps.stream()
            .skip(Math.max(0, emps.size() - 5))
            .sorted(Comparator.reverseOrder())
            .forEach(e -> vboxRecientes.getChildren().add(crearMiniItem(
                e.getDenominacionSocial(), e.getFechaAlta() != null ? e.getFechaAlta() : "—"
            )));
        if (vboxRecientes.getChildren().isEmpty())
            vboxRecientes.getChildren().add(crearMiniItem("Sin empresas registradas", ""));

        // Top personas
        vboxTopPersonas.getChildren().clear();
        pers.stream()
            .map(p -> Map.entry(p, DatabaseManager.obtenerEmpresasDePersona(p.getNif()).size()))
            .filter(e -> e.getValue() > 0)
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(5)
            .forEach(e -> vboxTopPersonas.getChildren().add(crearMiniItem(
                e.getKey().getNombreCompleto(),
                e.getValue() + " empresa" + (e.getValue() > 1 ? "s" : "")
            )));
        if (vboxTopPersonas.getChildren().isEmpty())
            vboxTopPersonas.getChildren().add(crearMiniItem("Sin datos de relaciones", ""));
    }

    // ─── Helpers UI ──────────────────────────────────────────────────────────

    /** Renderiza un conjunto de filas "etiqueta + barra + número" en un VBox. */
    private void renderBars(VBox container, List<String> labels, List<String> colors,
                            java.util.function.Function<String, Integer> countFn, int maxVal) {
        container.getChildren().clear();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            String color = colors.get(i);
            int count    = countFn.apply(label);

            Label lbl = new Label(label);
            lbl.getStyleClass().add("bar-label");
            lbl.setMinWidth(100);

            ProgressBar bar = new ProgressBar((double) count / maxVal);
            bar.getStyleClass().add("dash-bar");
            bar.setStyle("-fx-accent: " + color + ";");
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label num = new Label(String.valueOf(count));
            num.getStyleClass().add("bar-count");
            num.setMinWidth(28);

            HBox row = new HBox(10, lbl, bar, num);
            row.getStyleClass().add("bar-row");
            container.getChildren().add(row);
        }
    }

    /** Crea una tarjeta simple de lista (nombre + metadato). */
    private HBox crearMiniItem(String titulo, String meta) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("mini-item-label");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        Label lblMeta = new Label(meta);
        lblMeta.getStyleClass().add("mini-item-meta");

        HBox box = new HBox(10, lblTitulo, lblMeta);
        box.getStyleClass().add("mini-item");
        return box;
    }
}
