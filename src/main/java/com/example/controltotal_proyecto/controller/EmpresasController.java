package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.service.EmpresaService;
import com.example.controltotal_proyecto.util.BadgeFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador de la vista "Empresas".
 * Muestra las empresas como tarjetas (cards) en lugar de tabla,
 * y abre el formulario de alta/edición de forma INLINE dentro de la
 * misma ventana (sin Stage emergente).
 */
public class EmpresasController implements Initializable,
        MainController.RefreshableController, MainController.ChildController {

    // ─── Vistas (StackPane layers) ────────────────────────────────────────────
    @FXML private VBox vistaLista;        // Capa que muestra la lista de tarjetas
    @FXML private VBox vistaFormulario;   // Capa que muestra el formulario inline

    // ─── Contenedor de tarjetas ───────────────────────────────────────────────
    @FXML private VBox listContainer;     // VBox dentro del ScrollPane
    @FXML private VBox lblEmpty;          // Placeholder "no hay empresas"

    // ─── Filtros y búsqueda ───────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ToggleButton     chipTodos;
    @FXML private ToggleButton     chipActivo;
    @FXML private ToggleButton     chipPasivo;
    @FXML private ComboBox<String> comboServicio;
    @FXML private ComboBox<String> comboDelegacion;
    @FXML private ComboBox<String> comboAgente;

    // ─── Datos ────────────────────────────────────────────────────────────────
    private final EmpresaService          service      = new EmpresaService();
    private final ObservableList<Empresa> masterList   = FXCollections.observableArrayList();
    private       FilteredList<Empresa>   filteredList;
    private       MainController          mainController;

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        vistaLista.setPrefWidth(Double.MAX_VALUE);
        vistaLista.setPrefHeight(Double.MAX_VALUE);

        vistaFormulario.setPrefWidth(Double.MAX_VALUE);
        vistaFormulario.setPrefHeight(Double.MAX_VALUE);

        configurarFiltros();
        cargarDatos();
    }

    @Override public void onViewActivated()                       { cargarDatos(); }
    @Override public void setMainController(MainController main)  { this.mainController = main; }

    // ─── Configurar filtros ───────────────────────────────────────────────────

    private void configurarFiltros() {
        ToggleGroup tg = new ToggleGroup();
        chipTodos.setToggleGroup(tg);
        chipActivo.setToggleGroup(tg);
        chipPasivo.setToggleGroup(tg);
        chipTodos.setSelected(true);
        tg.selectedToggleProperty().addListener((obs, o, n) -> renderCards());

        comboServicio.getItems().add("Todos");
        comboServicio.getItems().addAll(service.getServicios());
        comboServicio.setValue("Todos");
        comboServicio.setOnAction(e -> renderCards());

        comboDelegacion.getItems().add("Todas");
        comboDelegacion.getItems().addAll(service.getDelegaciones());
        comboDelegacion.setValue("Todas");
        comboDelegacion.setOnAction(e -> renderCards());

        comboAgente.getItems().add("Todos");
        comboAgente.getItems().addAll(service.getAgentes());
        comboAgente.setValue("Todos");
        comboAgente.setOnAction(e -> renderCards());

        searchField.textProperty().addListener((obs, o, n) -> renderCards());
    }

    // ─── Carga y renderizado ──────────────────────────────────────────────────

    private void cargarDatos() {
        masterList.setAll(service.obtenerTodas());
        filteredList = new FilteredList<>(masterList, p -> true);
        renderCards();
    }

    /** Aplica los filtros activos y reconstruye las tarjetas visibles. */
    private void renderCards() {
        if (filteredList == null) return;
        aplicarFiltros();
        List<Empresa> visible = filteredList.stream().collect(Collectors.toList());

        listContainer.getChildren().clear();
        for (Empresa e : visible) {
            listContainer.getChildren().add(crearTarjeta(e));
        }
        actualizarEstadoVacio(visible.isEmpty());
    }

    private void aplicarFiltros() {
        filteredList.setPredicate(e -> {
            String q = searchField.getText().toLowerCase();
            boolean matchQ = q.isBlank()
                    || safe(e.getDenominacionSocial()).contains(q)
                    || safe(e.getNifCif()).contains(q)
                    || safe(e.getAbreviatura()).contains(q);

            ToggleButton sel = (ToggleButton) chipTodos.getToggleGroup().getSelectedToggle();
            boolean matchE = sel == null || sel == chipTodos
                    || (sel == chipActivo && e.isActivo())
                    || (sel == chipPasivo && !e.isActivo());

            String sv = comboServicio.getValue();
            boolean matchS = sv == null || sv.equals("Todos") || sv.equals(e.getServicio());

            String dv = comboDelegacion.getValue();
            boolean matchD = dv == null || dv.equals("Todas") || dv.equals(e.getDelegacion());

            String av = comboAgente.getValue();
            boolean matchA = av == null || av.equals("Todos") || av.equals(e.getAgenteContable());

            return matchQ && matchE && matchS && matchD && matchA;
        });
    }

    private void actualizarEstadoVacio(boolean vacio) {
        listContainer.setVisible(!vacio);
        lblEmpty.setVisible(vacio);
    }

    private String safe(String s) { return s != null ? s.toLowerCase() : ""; }

    // ─── Construcción de tarjeta ──────────────────────────────────────────────

    /**
     * Construye la tarjeta visual de una empresa, replicando el diseño de la captura:
     * [Monograma] [Denominación | Forma | NIF | Servicio | Delegación | Agente] [Estado] [Botones]
     */
    private Node crearTarjeta(Empresa e) {

        /* ── Monograma ── */
        String abr = (e.getAbreviatura() != null && !e.getAbreviatura().isBlank())
                ? e.getAbreviatura().substring(0, Math.min(10, e.getAbreviatura().length()))
                : iniciales(e.getDenominacionSocial());
        Label monogram = new Label(abr);
        monogram.getStyleClass().add("card-monogram");
        monogram.setMinWidth(76);
        monogram.setMinHeight(76);
        monogram.setMaxWidth(76);
        monogram.setMaxHeight(76);
        monogram.setAlignment(Pos.CENTER);

        /* ── Col 1: Denominación ── */
        VBox colDenom = campoCard("Denominación Social",
                e.getDenominacionSocial(), "card-field-value-bold");
        colDenom.setPrefWidth(250);
        colDenom.setMinWidth(140);

        /* ── Col 2: Forma Social ── */
        VBox colFS = campoCard("Forma Social",
                nvl(e.getFormaSocial()), "card-field-value");
        colFS.setPrefWidth(120);

        /* ── Col 3: NIF/CIF ── */
        VBox colNif = campoCard("NIF/CIF",
                nvl(e.getNifCif()), "card-field-value-mono");
        colNif.setPrefWidth(100);

        /* ── Col 4: Servicio (badge) ── */
        Label lblSrvTitle = new Label("Servicio");
        lblSrvTitle.getStyleClass().add("card-field-title");
        Node srvBadge = BadgeFactory.servicioBadge(nvl(e.getServicio()));
        VBox colSrv = new VBox(4, lblSrvTitle, srvBadge);
        colSrv.setPrefWidth(100);

        /* ── Col 5: Delegación ── */
        VBox colDel = campoCard("Delegación",
                nvl(e.getDelegacion()), "card-field-value");
        colDel.setPrefWidth(150);

        /* ── Col 6: Agente Contable ── */
        VBox colAgt = campoCard("Agente Contable",
                nvl(e.getAgenteContable()), "card-field-value");
        HBox.setHgrow(colAgt, Priority.ALWAYS);

        /* ── Fila de campos ── */
        HBox fields = new HBox(16, colDenom, colFS, colNif, colSrv, colDel, colAgt);
        fields.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fields, Priority.ALWAYS);

        /* ── Estado badge ── */
        Node estadoBadge = BadgeFactory.estadoBadge(e.isActivo() ? "Activo" : "Pasivo");

        /* ── Botones ── */
        Button btnMod = new Button("✏  Modificar");
        btnMod.getStyleClass().add("btn-card-edit");
        btnMod.setOnAction(ev -> abrirFormulario(e));

        Button btnCarp = new Button("📁  Abrir Directorios");
        btnCarp.getStyleClass().add("btn-card-folder");
        btnCarp.setOnAction(ev -> abrirCarpeta(e.getRutaDocumental()));

        HBox btnRow = new HBox(8, btnMod, btnCarp);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        /* ── Área derecha: badge + botones ── */
        VBox rightArea = new VBox(10, estadoBadge, btnRow);
        rightArea.setAlignment(Pos.TOP_RIGHT);

        /* ── Contenido central + derecha ── */
        HBox centerAndRight = new HBox(16, fields, rightArea);
        centerAndRight.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fields, Priority.ALWAYS);

        /* ── Tarjeta completa ── */
        HBox card = new HBox(16, monogram, centerAndRight);
        card.getStyleClass().add("empresa-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 16));
        HBox.setHgrow(centerAndRight, Priority.ALWAYS);

        return card;
    }

    /** Helper: crea un VBox con título gris + valor. */
    private VBox campoCard(String titulo, String valor, String valueStyleClass) {
        Label t = new Label(titulo);
        t.getStyleClass().add("card-field-title");
        Label v = new Label(valor);
        v.getStyleClass().add(valueStyleClass);
        return new VBox(4, t, v);
    }

    /** Genera iniciales a partir del nombre (máx. 2 letras). */
    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (!p.isBlank()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }

    /** Devuelve "—" si el valor es nulo o vacío. */
    private String nvl(String s) { return (s != null && !s.isBlank()) ? s : "—"; }

    // ─── Formulario inline ────────────────────────────────────────────────────

    @FXML private void onNuevaEmpresa() { abrirFormulario(null); }

    /**
     * Carga EmpresaFormDialog.fxml en la capa 'vistaFormulario'
     * y oculta la lista — sin abrir ningún Stage.
     */
    private void abrirFormulario(Empresa empresaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/controltotal_proyecto/fxml/dialogs/EmpresaFormDialog.fxml")
            );
            Node formRoot = loader.load();
            EmpresaFormController ctrl = loader.getController();

            // Runnable que vuelve a la lista cuando el form termina
            Runnable volver = () -> {
                vistaFormulario.setVisible(false);
                vistaFormulario.getChildren().clear();
                vistaLista.setVisible(true);
                cargarDatos();
                if (mainController != null) mainController.refreshStats();
            };

            ctrl.setCloseAction(volver);
            ctrl.init(empresaEditar, result -> volver.run());

            if (formRoot instanceof Region region) {
                region.prefWidthProperty().bind(vistaFormulario.widthProperty());
                region.prefHeightProperty().bind(vistaFormulario.heightProperty());
            }

            VBox.setVgrow(formRoot, Priority.ALWAYS);

            vistaFormulario.getChildren().setAll(formRoot);
            vistaLista.setVisible(false);
            vistaFormulario.setVisible(true);

        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo cargar el formulario de empresa.").show();
        }
    }

    private void abrirCarpeta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Esta empresa no tiene ruta asignada.").show();
            return;
        }
        try {
            new ProcessBuilder("explorer.exe", ruta).start();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.INFORMATION, "Ruta: " + ruta).show();
        }
    }
}
