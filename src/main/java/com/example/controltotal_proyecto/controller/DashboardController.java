package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController implements Initializable,
        MainController.RefreshableController, MainController.ChildController {

    @FXML private Label lblTotalEmpresas;
    @FXML private Label lblActivasEmpresas;
    @FXML private Label lblPasivasEmpresas;
    @FXML private Label lblTotalPersonas;

    @FXML private VBox  vboxServiciosBars;
    @FXML private HBox  hboxServiciosAxis;

    @FXML private VBox  vboxDelegBars;
    @FXML private HBox  hboxDelegAxis;

    @FXML private PieChart chartAgente;
    @FXML private VBox     vboxAgenteLegend;

    @FXML private TableView<Empresa> tableRecientes;

    private MainController mainController;

    private static final List<String> PIE_COLORS = List.of(
            "#4f7dff", "#a78bfa", "#3ecf8e", "#f5a623", "#f06060",
            "#c9a84c", "#60c4f0", "#f07830"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        inicializarTabla();
        refrescar();
    }

    @Override public void onViewActivated()                       { refrescar(); }
    @Override public void setMainController(MainController main)  { this.mainController = main; }

    @SuppressWarnings("unchecked")
    private void inicializarTabla() {
        tableRecientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Empresa, String> colAbr = col("Abrvi",       "abreviatura",        60);
        TableColumn<Empresa, String> colNom = col("Nombre",      "denominacionSocial", 140);
        TableColumn<Empresa, String> colFor = col("Forma",       "formaSocial",        80); // Corregido
        TableColumn<Empresa, String> colFec = col("Fecha Alta",  "fechaAlta",          100);
        TableColumn<Empresa, String> colSrv = col("Servicio",    "servicio",            90);
        TableColumn<Empresa, String> colAge = col("Agente",      "agenteContable",     110); // Corregido
        TableColumn<Empresa, String> colDel = col("Delegación",  "delegacion",         110);

        TableColumn<Empresa, Boolean> colEst = new TableColumn<>("Estado");
        colEst.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colEst.setMinWidth(80);
        colEst.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Boolean activo, boolean empty) {
                super.updateItem(activo, empty);
                if (empty || activo == null) { setGraphic(null); return; }
                String texto = activo ? "Activo" : "Pasivo";
                Label badge = new Label(texto);
                badge.getStyleClass().addAll("badge-estado",
                        activo ? "badge-activo" : "badge-pasivo");
                setGraphic(badge);
                setText(null);
            }
        });

        tableRecientes.getColumns().setAll(colAbr, colNom, colFor, colFec, colSrv, colAge, colDel, colEst);
        tableRecientes.setPlaceholder(new Label("Sin empresas registradas"));
    }

    private TableColumn<Empresa, String> col(String titulo, String propiedad, double minW) {
        TableColumn<Empresa, String> tc = new TableColumn<>(titulo);
        tc.setCellValueFactory(new PropertyValueFactory<>(propiedad));
        tc.setMinWidth(minW);
        return tc;
    }

    private void refrescar() {
        List<Empresa> emps = DatabaseManager.obtenerTodasLasEmpresas();
        List<Persona> pers = DatabaseManager.obtenerTodasLasPersonas();

        if (emps == null) emps = new ArrayList<>();
        if (pers == null) pers = new ArrayList<>();

        long activas = emps.stream().filter(Empresa::isActivo).count();
        long pasivas  = emps.size() - activas;

        lblTotalEmpresas.setText(String.valueOf(emps.size()));
        lblActivasEmpresas.setText(String.valueOf(activas));
        lblPasivasEmpresas.setText(String.valueOf(pasivas));
        lblTotalPersonas.setText(String.valueOf(pers.size()));

        // Barras Servicio
        List<String> servicios  = List.of("Auditoría", "Asesoría", "Pericial", "Concursal");
        List<String> colorsSrv  = List.of("#4f7dff", "#f5a623", "#3ecf8e", "#a78bfa");
        final List<Empresa> finalEmps = emps;
        int maxSrv = servicios.stream()
                .mapToInt(s -> (int) finalEmps.stream().filter(e -> s.equalsIgnoreCase(e.getServicio())).count())
                .max().orElse(1);
        renderBars(vboxServiciosBars, hboxServiciosAxis, servicios, colorsSrv,
                s -> (int) finalEmps.stream().filter(e -> s.equalsIgnoreCase(e.getServicio())).count(), maxSrv);

        // Barras Delegación
        List<String> delegs    = List.of("Huelva", "Lepe", "Puebla de Guzmán");
        List<String> colorsDel = List.of("#4f7dff", "#f5a623", "#3ecf8e");
        int maxDel = delegs.stream()
                .mapToInt(d -> (int) finalEmps.stream().filter(e -> d.equalsIgnoreCase(e.getDelegacion())).count())
                .max().orElse(1);
        renderBars(vboxDelegBars, hboxDelegAxis, delegs, colorsDel,
                d -> (int) finalEmps.stream().filter(e -> d.equalsIgnoreCase(e.getDelegacion())).count(), maxDel);

        renderPieAgente(emps);

        // Corregido: La ordenación de objetos ahora es explícita para evitar crasheos
        List<Empresa> recientes = emps.stream()
                .sorted(Comparator.comparing(Empresa::getDenominacionSocial).reversed())
                .limit(8)
                .collect(Collectors.toList());
        tableRecientes.setItems(FXCollections.observableArrayList(recientes));
    }

    private void renderPieAgente(List<Empresa> emps) {
        Map<String, Long> porAgente = emps.stream()
                .filter(e -> e.getAgenteContable() != null && !e.getAgenteContable().isBlank())
                .collect(Collectors.groupingBy(Empresa::getAgenteContable, Collectors.counting()));

        List<Map.Entry<String, Long>> sorted = porAgente.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        javafx.collections.ObservableList<PieChart.Data> nuevosDatosGrafico = javafx.collections.FXCollections.observableArrayList();
        List<javafx.scene.Node> nuevosNodosLeyenda = new ArrayList<>();

        long total = sorted.stream().mapToLong(Map.Entry::getValue).sum();

        if (sorted.isEmpty()) {
            Label emptyLbl = new Label("Sin datos de agentes");
            emptyLbl.getStyleClass().add("legend-label");
            nuevosNodosLeyenda.add(emptyLbl);
        } else {
            for (int i = 0; i < sorted.size(); i++) {
                Map.Entry<String, Long> entry = sorted.get(i);
                String color  = PIE_COLORS.get(i % PIE_COLORS.size());
                String nombre = entry.getKey();
                long   cnt    = entry.getValue();

                // Datos para el gráfico
                String textoGrafico = String.format("%s (%d)", nombre, cnt);
                PieChart.Data slice = new PieChart.Data(textoGrafico, cnt);

                // Aplicar el color de forma segura
                slice.nodeProperty().addListener((obs, o, node) -> {
                    if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
                });
                nuevosDatosGrafico.add(slice);

                // Datos para la leyenda lateral
                Circle dot = new Circle(7, javafx.scene.paint.Color.web(color));
                double pct = total > 0 ? (cnt * 100.0 / total) : 0;
                Label lbl  = new Label(String.format("%s (%.1f%%, %d)", nombre, pct, cnt));
                lbl.getStyleClass().add("legend-label");

                HBox row = new HBox(8, dot, lbl);
                row.setAlignment(Pos.CENTER_LEFT);
                nuevosNodosLeyenda.add(row);
            }
        }

        // ── FIX NUCLEAR: Reemplazar el gráfico por uno nuevo ──
        javafx.application.Platform.runLater(() -> {
            // Obtenemos el contenedor padre donde está el gráfico (el HBox)
            HBox parent = (HBox) chartAgente.getParent();
            int index = parent.getChildren().indexOf(chartAgente);

            // Creamos un gráfico desde cero
            PieChart nuevoChart = new PieChart();
            nuevoChart.setAnimated(false);
            nuevoChart.setLegendVisible(false);

            // ¡ACTIVAMOS LAS RAYAS AQUÍ!
            nuevoChart.setLabelsVisible(true);
            nuevoChart.setLabelLineLength(15);

            nuevoChart.setPrefSize(350, 280);
            HBox.setHgrow(nuevoChart, Priority.ALWAYS);

            // Le metemos los datos
            nuevoChart.setData(nuevosDatosGrafico);

            // Reemplazamos el gráfico corrupto por el nuevo en la vista
            if (index != -1) {
                parent.getChildren().set(index, nuevoChart);
            } else {
                parent.getChildren().add(0, nuevoChart);
            }

            // Actualizamos la variable global de nuestro controlador
            chartAgente = nuevoChart;

            // Ponemos la leyenda
            vboxAgenteLegend.getChildren().setAll(nuevosNodosLeyenda);
        });
    }

    private void renderBars(VBox container, HBox axisBox,
                            List<String> labels, List<String> colors,
                            java.util.function.Function<String, Integer> countFn,
                            int maxVal) {
        container.getChildren().clear();
        final double BAR_MAX_PX = 250.0; // Ajustado para evitar desbordamiento

        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            String color = colors.get(i);
            int    count = countFn.apply(label);

            Label lblName = new Label(label);
            lblName.getStyleClass().add("bar-label");
            lblName.setMinWidth(140);
            lblName.setMaxWidth(140);

            double pxWidth = (maxVal > 0) ? (count / (double) maxVal) * BAR_MAX_PX : 0;
            Region bar = new Region();
            bar.setPrefHeight(18);
            bar.setMinWidth(Math.max(pxWidth, 2));
            bar.setPrefWidth(Math.max(pxWidth, 2));
            bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");

            Label lblNum = new Label(String.valueOf(count));
            lblNum.getStyleClass().add("bar-count");

            HBox row = new HBox(10, lblName, bar, lblNum);
            row.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().add(row);
        }
    }
}