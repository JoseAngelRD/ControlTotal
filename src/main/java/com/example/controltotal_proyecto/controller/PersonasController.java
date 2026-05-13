package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.service.PersonaService;
import com.example.controltotal_proyecto.util.BadgeFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la vista "Personas" — diseño de tarjetas al estilo Empresas.
 */
public class PersonasController implements Initializable,
        MainController.RefreshableController, MainController.ChildController {

    // ─── FXML ─────────────────────────────────────────────────────────────────
    @FXML private TextField    searchField;
    @FXML private ToggleButton chipTodos;
    @FXML private ToggleButton chipActivo;
    @FXML private ToggleButton chipInactivo;

    @FXML private VBox listContainer;
    @FXML private VBox lblEmpty;

    // ─── Datos ────────────────────────────────────────────────────────────────
    private final PersonaService          service    = new PersonaService();
    private final ObservableList<Persona> masterList = FXCollections.observableArrayList();
    private       FilteredList<Persona>   filtered;
    private       MainController          mainController;

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFiltros();
        cargarDatos();
    }

    @Override public void onViewActivated()                      { cargarDatos(); }
    @Override public void setMainController(MainController main) { this.mainController = main; }

    // ─── Filtros ──────────────────────────────────────────────────────────────

    private void configurarFiltros() {
        ToggleGroup tg = new ToggleGroup();
        chipTodos.setToggleGroup(tg);
        chipActivo.setToggleGroup(tg);
        chipInactivo.setToggleGroup(tg);
        chipTodos.setSelected(true);
        tg.selectedToggleProperty().addListener((obs, o, n) -> renderizarFiltrado());
        searchField.textProperty().addListener((obs, o, n) -> renderizarFiltrado());
    }

    // ─── Carga y renderizado ──────────────────────────────────────────────────

    private void cargarDatos() {
        masterList.setAll(service.obtenerTodas());
        filtered = new FilteredList<>(masterList, p -> true);
        renderizarFiltrado();
    }

    private void renderizarFiltrado() {
        if (filtered == null) return;

        filtered.setPredicate(p -> {
            String q = searchField.getText().toLowerCase();
            boolean mQ = q.isBlank() ||
                    p.getNombreCompleto().toLowerCase().contains(q) ||
                    safe(p.getNif()).contains(q) ||
                    safe(p.getContactoMail()).contains(q);

            ToggleButton sel = (ToggleButton) chipTodos.getToggleGroup().getSelectedToggle();
            boolean mE = sel == null || sel == chipTodos ||
                    (sel == chipActivo   && p.isActivo()) ||
                    (sel == chipInactivo && !p.isActivo());

            return mQ && mE;
        });

        listContainer.getChildren().clear();
        filtered.forEach(p -> listContainer.getChildren().add(crearTarjeta(p)));

        boolean vacio = filtered.isEmpty();
        listContainer.setVisible(!vacio);
        lblEmpty.setVisible(vacio);
    }

    // ─── Tarjeta ──────────────────────────────────────────────────────────────

    private HBox crearTarjeta(Persona p) {

        // ── 2. Columnas de datos: etiqueta encima, valor debajo ───────────────
        VBox colNombre    = campoVertical("Nombre",    p.getNombre(),        false, false);
        VBox colApellidos = campoVertical("Apellidos", p.getApellidos(),     false, false);
        VBox colNif       = campoVertical("NIF",       p.getNif(),           false, true);
        VBox colMovil     = campoVertical("Móvil",     p.getContactoMovil(), false, false);
        VBox colEmail     = campoVertical("Email",     p.getContactoMail(),  false, false);

        HBox campos = new HBox(24, colNombre, colApellidos, colNif, colMovil, colEmail);
        campos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campos, Priority.ALWAYS);

        // ── 3. Empresas relacionadas ──────────────────────────────────────────
        List<Empresa> empresas = DatabaseManager.obtenerEmpresasDePersona(p.getNif());

        Label lblEmpresasTitle = new Label("Empresas");
        lblEmpresasTitle.getStyleClass().add("card-field-title");

        FlowPane flowEmpresas = new FlowPane(4, 4);
        flowEmpresas.setAlignment(Pos.CENTER_LEFT);
        if (empresas.isEmpty()) {
            Label sinEmp = new Label("Sin empresas");
            sinEmp.getStyleClass().add("text-muted");
            flowEmpresas.getChildren().add(sinEmp);
        } else {
            empresas.forEach(e -> {
                if (e != null && e.getAbreviatura() != null) {
                    flowEmpresas.getChildren().add(BadgeFactory.empresaTag(e.getAbreviatura()));
                }
            });
        }

        VBox empresasBox = new VBox(4, lblEmpresasTitle, flowEmpresas);
        empresasBox.setAlignment(Pos.TOP_LEFT);
        empresasBox.setMinWidth(160);
        empresasBox.setMaxWidth(220);

        // ── 4. Badge de estado + botones, alineados arriba a la derecha ───────
        String estadoTexto = p.isActivo() ? "Activo" : "Pasivo";
        Label badgeEstado = BadgeFactory.estadoBadge(estadoTexto);

        Button btnEditar = new Button("✏ Modificar");
        btnEditar.getStyleClass().add("btn-card-edit");
        btnEditar.setOnAction(e -> abrirFormulario(p));

        Button btnCarpeta = new Button("📁 Carpeta");
        btnCarpeta.getStyleClass().add("btn-card-folder");
        btnCarpeta.setOnAction(e -> abrirCarpeta(p.getRutaDocumental()));

        HBox botonesBox = new HBox(8, btnEditar, btnCarpeta);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);

        // Badge arriba a la derecha, botones debajo también a la derecha
        VBox accionesBox = new VBox(8, badgeEstado, botonesBox);
        accionesBox.setAlignment(Pos.TOP_RIGHT);
        accionesBox.setMinWidth(220);

        // ── 5. Tarjeta completa ───────────────────────────────────────────────
        HBox card = new HBox(16, campos, empresasBox, accionesBox);
        card.getStyleClass().add("empresa-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 20, 14, 16));

        return card;
    }

    /**
     * Campo vertical: etiqueta pequeña arriba (card-field-title) + valor legible abajo.
     * bold=true → card-field-value-bold; mono=true → card-field-value-mono;
     * en caso contrario → card-field-value (color claro #cbd5e1).
     */
    private VBox campoVertical(String etiqueta, String valor, boolean bold, boolean mono) {
        Label lblTitle = new Label(etiqueta);
        lblTitle.getStyleClass().add("card-field-title");

        String texto = (valor != null && !valor.isBlank()) ? valor : "—";
        Label lblValue = new Label(texto);
        if (bold)       lblValue.getStyleClass().add("card-field-value-bold");
        else if (mono)  lblValue.getStyleClass().add("card-field-value-mono");
        else            lblValue.getStyleClass().add("card-field-value");

        VBox box = new VBox(2, lblTitle, lblValue);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    /** Iniciales: primera letra de nombre + primera de apellidos. */

    private String safe(String s) { return s != null ? s.toLowerCase() : ""; }

    // ─── Acciones ─────────────────────────────────────────────────────────────

    @FXML private void onNuevaPersona() { abrirFormulario(null); }

    private void abrirFormulario(Persona personaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/controltotal_proyecto/fxml/dialogs/PersonaFormDialog.fxml")
            );
            Parent root = loader.load();
            PersonaFormController ctrl = loader.getController();
            ctrl.init(personaEditar, result -> {
                cargarDatos();
                if (mainController != null) mainController.refreshStats();
            });

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(personaEditar == null ? "Nueva Persona" : "Editar Persona");
            dialog.setScene(new Scene(root));
            dialog.getScene().getStylesheets().add(
                    getClass().getResource("/com/example/controltotal_proyecto/css/styles.css").toExternalForm()
            );
            dialog.setMinWidth(520);
            dialog.show();

        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario.").show();
        }
    }

    private void abrirCarpeta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Esta persona no tiene ruta asignada.").show();
            return;
        }
        try {
            new ProcessBuilder("explorer.exe", ruta).start();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.INFORMATION, "Ruta: " + ruta).show();
        }
    }
}
