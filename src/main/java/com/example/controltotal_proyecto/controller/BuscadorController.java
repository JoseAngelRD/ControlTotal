package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.service.EmpresaService;
import com.example.controltotal_proyecto.util.BadgeFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador de la vista "Buscador Avanzado".
 * Permite buscar empresas y personas de forma combinada.
 * Incluye filtro por agente contable.
 */
public class BuscadorController implements Initializable,
        MainController.RefreshableController, MainController.ChildController {

    @FXML private TextField        searchField;
    @FXML private ToggleButton     chipTodos;
    @FXML private ToggleButton     chipEmpresas;
    @FXML private ToggleButton     chipPersonas;
    @FXML private ComboBox<String> comboServicio;
    @FXML private ComboBox<String> comboDelegacion;
    @FXML private ComboBox<String> comboEstado;
    @FXML private ComboBox<String> comboAgente;   // ← nuevo filtro
    @FXML private VBox             resultContainer;

    private MainController    mainController;
    private final EmpresaService empService = new EmpresaService();

    // Valor centinela para "sin filtro"
    private static final String TODOS_AGENTES = "Todos los agentes";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ── Grupo de chips ──
        ToggleGroup tg = new ToggleGroup();
        chipTodos.setToggleGroup(tg);
        chipEmpresas.setToggleGroup(tg);
        chipPersonas.setToggleGroup(tg);
        chipTodos.setSelected(true);

        // ── Servicio ──
        comboServicio.getItems().add("Todos");
        comboServicio.getItems().addAll(empService.getServicios());
        comboServicio.setValue("Todos");

        // ── Delegación ──
        comboDelegacion.getItems().add("Todas");
        comboDelegacion.getItems().addAll(empService.getDelegaciones());
        comboDelegacion.setValue("Todas");

        // ── Estado ──
        comboEstado.getItems().addAll("Cualquier estado", "Activo", "Pasivo");
        comboEstado.setValue("Cualquier estado");

        // ── Agente contable: cargado dinámicamente desde las empresas ──
        cargarAgentes();

        // ── Listeners reactivos ──
        searchField.textProperty().addListener((obs, o, n) -> buscar());
        tg.selectedToggleProperty().addListener((obs, o, n) -> buscar());
        comboServicio.setOnAction(e -> buscar());
        comboDelegacion.setOnAction(e -> buscar());
        comboEstado.setOnAction(e -> buscar());
        comboAgente.setOnAction(e -> buscar());

        mostrarPlaceholder();
    }

    /** Rellena el ComboBox de agentes con los valores únicos de la BD. */
    private void cargarAgentes() {
        List<String> agentes = DatabaseManager.obtenerTodasLasEmpresas().stream()
                .map(Empresa::getAgenteContable)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        comboAgente.getItems().clear();
        comboAgente.getItems().add(TODOS_AGENTES);
        comboAgente.getItems().addAll(agentes);
        comboAgente.setValue(TODOS_AGENTES);
    }

    @Override
    public void onViewActivated() {
        // Recargar agentes por si hay nuevas empresas
        String agenteActual = comboAgente.getValue();
        cargarAgentes();
        if (agenteActual != null && comboAgente.getItems().contains(agenteActual)) {
            comboAgente.setValue(agenteActual);
        }
        buscar();
    }

    @Override public void setMainController(MainController main) { this.mainController = main; }

    @FXML private void onBuscar() { buscar(); }

    // ─── Lógica de búsqueda ───────────────────────────────────────────────────

    private void buscar() {
        String q      = searchField.getText().toLowerCase().trim();
        String tipo   = chipEmpresas.isSelected() ? "empresa"
                : chipPersonas.isSelected() ? "persona" : "";

        // ─── CORRECCIÓN ──────────────────────────────────────────────────────
        // Declaramos las variables como 'final' o asignamos su valor por defecto
        // directamente en la misma línea. Así evitamos reasignarlas.
        final String sv     = comboServicio.getValue() == null ? "Todos" : comboServicio.getValue();
        final String dv     = comboDelegacion.getValue() == null ? "Todas" : comboDelegacion.getValue();
        final String ev     = comboEstado.getValue() == null ? "Cualquier estado" : comboEstado.getValue();
        final String agente = comboAgente.getValue() == null ? TODOS_AGENTES : comboAgente.getValue();
        // ─────────────────────────────────────────────────────────────────────

        boolean sinFiltros = q.isBlank()
                && tipo.isBlank()
                && "Todos".equals(sv)
                && "Todas".equals(dv)
                && "Cualquier estado".equals(ev)
                && TODOS_AGENTES.equals(agente);

        if (sinFiltros) {
            mostrarPlaceholder();
            return;
        }

        List<Object[]> resultados = new ArrayList<>();

        // ── Empresas ──
        if (tipo.isEmpty() || tipo.equals("empresa")) {
            DatabaseManager.obtenerTodasLasEmpresas().stream()
                    .filter(e -> q.isBlank()
                            || safe(e.getDenominacionSocial()).contains(q)
                            || safe(e.getNifCif()).contains(q)
                            || safe(e.getAbreviatura()).contains(q))
                    .filter(e -> "Todos".equals(sv) || sv.equals(e.getServicio()))
                    .filter(e -> "Todas".equals(dv) || dv.equals(e.getDelegacion()))
                    .filter(e -> "Cualquier estado".equals(ev)
                            || ("Activo".equals(ev)  &&  e.isActivo())
                            || ("Pasivo".equals(ev)  && !e.isActivo()))
                    .filter(e -> TODOS_AGENTES.equals(agente)
                            || agente.equals(e.getAgenteContable()))
                    .forEach(e -> resultados.add(new Object[]{"empresa", e}));
        }

        // ── Personas ──
        if (tipo.isEmpty() || tipo.equals("persona")) {
            DatabaseManager.obtenerTodasLasPersonas().stream()
                    .filter(p -> q.isBlank()
                            || p.getNombreCompleto().toLowerCase().contains(q)
                            || safe(p.getNif()).contains(q)
                            || safe(p.getContactoMail()).contains(q))
                    .filter(p -> "Cualquier estado".equals(ev)
                            || ("Activo".equals(ev)  &&  p.isActivo())
                            || ("Pasivo".equals(ev)  && !p.isActivo()))
                    // Filtro agente sobre personas: mostrar personas vinculadas al agente
                    .filter(p -> TODOS_AGENTES.equals(agente)
                            || DatabaseManager.obtenerEmpresasDePersona(p.getNif()).stream()
                            .anyMatch(emp -> agente.equals(emp.getAgenteContable())))
                    .forEach(p -> resultados.add(new Object[]{"persona", p}));
        }

        renderResultados(resultados);
    }

    private void renderResultados(List<Object[]> resultados) {
        resultContainer.getChildren().clear();

        if (resultados.isEmpty()) {
            Label lbl = new Label("Sin resultados para esos criterios.");
            lbl.getStyleClass().add("empty-hint");
            resultContainer.getChildren().add(lbl);
            return;
        }

        Label header = new Label(resultados.size() + " resultado"
                + (resultados.size() > 1 ? "s" : ""));
        header.getStyleClass().add("results-count");
        resultContainer.getChildren().add(header);

        for (Object[] row : resultados) {
            resultContainer.getChildren().add(
                    "empresa".equals(row[0])
                            ? crearFilaEmpresa((Empresa) row[1])
                            : crearFilaPersona((Persona)  row[1])
            );
        }
    }

    // ─── Filas de resultado ───────────────────────────────────────────────────

    private HBox crearFilaEmpresa(Empresa e) {
        Label tipo = new Label("Empresa");
        tipo.getStyleClass().addAll("badge-service",
                "badge-service-" + safe(e.getServicio()));
        tipo.setMinWidth(70);

        Label nombre = new Label(e.getDenominacionSocial());
        nombre.getStyleClass().add("result-nombre");
        Label abr = new Label("(" + e.getAbreviatura() + ")");
        abr.getStyleClass().add("result-meta");

        HBox nombreBox = new HBox(6, nombre, abr);
        nombreBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nombreBox, Priority.ALWAYS);

        Label nif  = new Label(e.getNifCif());
        nif.getStyleClass().add("result-nif");

        // Mostrar agente si está disponible
        String agenteStr = e.getAgenteContable() != null && !e.getAgenteContable().isBlank()
                ? e.getServicio() + " · " + e.getDelegacion() + " · " + e.getAgenteContable()
                : e.getServicio() + " · " + e.getDelegacion();
        Label info = new Label(agenteStr);
        info.getStyleClass().add("result-meta");

        Label est   = BadgeFactory.estadoBadge(e.getEstado());
        Button btn  = crearBtn("btn-icon-edit");
        btn.setOnAction(ev -> abrirFormEmpresa(e));

        HBox row = new HBox(10, tipo, nombreBox, nif, info, est, btn);
        row.getStyleClass().add("result-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox crearFilaPersona(Persona p) {
        Label tipo = new Label("Persona");
        tipo.getStyleClass().add("badge-persona");
        tipo.setMinWidth(70);

        Label nombre = new Label(p.getNombreCompleto());
        nombre.getStyleClass().add("result-nombre");
        HBox.setHgrow(nombre, Priority.ALWAYS);

        Label nif  = new Label(p.getNif());
        nif.getStyleClass().add("result-nif");
        Label info = new Label(p.getContactoMovil() + " · " + p.getContactoMail());
        info.getStyleClass().add("result-meta");
        Label est  = BadgeFactory.estadoBadge(p.getEstado());

        Button btn = crearBtn("btn-icon-edit");
        btn.setOnAction(ev -> abrirFormPersona(p));

        HBox row = new HBox(10, tipo, nombre, nif, info, est, btn);
        row.getStyleClass().add("result-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button crearBtn(String styleClass) {
        Button btn = new Button();
        btn.getStyleClass().addAll("btn-icon", styleClass);
        return btn;
    }

    private void mostrarPlaceholder() {
        resultContainer.getChildren().clear();
        Label lbl = new Label("Introduce un término de búsqueda o aplica filtros.");
        lbl.getStyleClass().add("empty-hint");
        resultContainer.getChildren().add(lbl);
    }

    private String safe(String s) { return s != null ? s.toLowerCase() : ""; }

    // ─── Abrir formularios ────────────────────────────────────────────────────

    private void abrirFormEmpresa(Empresa e) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/controltotal_proyecto/fxml/dialogs/EmpresaFormDialog.fxml"));
            Parent root = loader.load();
            EmpresaFormController ctrl = loader.getController();
            ctrl.init(e, result -> buscar());
            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Editar Empresa");
            st.setScene(new Scene(root));
            st.getScene().getStylesheets().add(
                    getClass().getResource(
                                    "/com/example/controltotal_proyecto/css/styles.css")
                            .toExternalForm());
            st.show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void abrirFormPersona(Persona p) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/controltotal_proyecto/fxml/dialogs/PersonaFormDialog.fxml"));
            Parent root = loader.load();
            PersonaFormController ctrl = loader.getController();
            ctrl.init(p, result -> buscar());
            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Editar Persona");
            st.setScene(new Scene(root));
            st.getScene().getStylesheets().add(
                    getClass().getResource(
                                    "/com/example/controltotal_proyecto/css/styles.css")
                            .toExternalForm());
            st.show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }
}
