package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.service.EmpresaService;
import com.example.controltotal_proyecto.service.PersonaService;
import com.example.controltotal_proyecto.util.BadgeFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador de la vista "Empresas".
 * Gestiona la tabla con filtros, búsqueda y apertura del diálogo de alta/edición.
 */
public class EmpresasController implements Initializable,
    MainController.RefreshableController, MainController.ChildController {

    // ─── Tabla ────────────────────────────────────────────────────────────────
    @FXML private TableView<Empresa>         tablaEmpresas;
    @FXML private TableColumn<Empresa,String> colAbreviatura;
    @FXML private TableColumn<Empresa,String> colDenominacion;
    @FXML private TableColumn<Empresa,String> colNif;
    @FXML private TableColumn<Empresa,String> colFormaSocial;
    @FXML private TableColumn<Empresa,String> colServicio;
    @FXML private TableColumn<Empresa,String> colDelegacion;
    @FXML private TableColumn<Empresa,String> colAgente;
    @FXML private TableColumn<Empresa,String> colEstado;
    @FXML private TableColumn<Empresa,Void>   colAcciones;

    // ─── Filtros y búsqueda ───────────────────────────────────────────────────
    @FXML private TextField      searchField;
    @FXML private ToggleButton   chipTodos;
    @FXML private ToggleButton   chipActivo;
    @FXML private ToggleButton   chipPasivo;
    @FXML private ComboBox<String> comboServicio;
    @FXML private ComboBox<String> comboDelegacion;
    @FXML private ComboBox<String> comboAgente;

    // ─── Estado vacío ─────────────────────────────────────────────────────────
    @FXML private VBox lblEmpty;

    // ─── Datos ───────────────────────────────────────────────────────────────
    private final EmpresaService             service      = new EmpresaService();
    private final ObservableList<Empresa>    masterList   = FXCollections.observableArrayList();
    private       FilteredList<Empresa>      filteredList;
    private       MainController             mainController;

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();
        configurarFiltros();
        cargarDatos();
    }

    @Override public void onViewActivated()                      { cargarDatos(); }
    @Override public void setMainController(MainController main) { this.mainController = main; }

    // ─── Configurar columnas ─────────────────────────────────────────────────

    private void configurarColumnas() {
        colAbreviatura.setCellValueFactory(new PropertyValueFactory<>("abreviatura"));
        colAbreviatura.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(v);
                lbl.getStyleClass().add("abr-label");
                setGraphic(lbl); setText(null);
            }
        });

        colDenominacion.setCellValueFactory(new PropertyValueFactory<>("denominacionSocial"));
        colDenominacion.getStyleClass().add("col-bold");

        colNif.setCellValueFactory(new PropertyValueFactory<>("nifCif"));
        colNif.getStyleClass().add("col-mono");

        colFormaSocial.setCellValueFactory(new PropertyValueFactory<>("formaSocial"));

        // Columna Servicio — badge con color
        colServicio.setCellValueFactory(new PropertyValueFactory<>("servicio"));
        colServicio.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                setGraphic(BadgeFactory.servicioBadge(v)); setText(null);
            }
        });

        colDelegacion.setCellValueFactory(new PropertyValueFactory<>("delegacion"));
        colAgente.setCellValueFactory(new PropertyValueFactory<>("agenteContable"));

        // Columna Estado — badge activo/pasivo
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                setGraphic(BadgeFactory.estadoBadge(v)); setText(null);
            }
        });

        // Columna Acciones — botones Editar / Carpeta
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar  = crearBotonIcono("Editar", "btn-icon-edit");
            private final Button btnCarpeta = crearBotonIcono("Carpeta", "btn-icon-folder");
            private final HBox   box        = new HBox(6, btnEditar, btnCarpeta);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnEditar.setOnAction(e -> {
                    Empresa emp = getTableView().getItems().get(getIndex());
                    abrirFormulario(emp);
                });
                btnCarpeta.setOnAction(e -> {
                    Empresa emp = getTableView().getItems().get(getIndex());
                    abrirCarpeta(emp.getRutaDocumental());
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablaEmpresas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private Button crearBotonIcono(String tooltip, String styleClass) {
        Button btn = new Button();
        btn.setTooltip(new Tooltip(tooltip));
        btn.getStyleClass().addAll("btn-icon", styleClass);
        return btn;
    }

    // ─── Configurar filtros ───────────────────────────────────────────────────

    private void configurarFiltros() {
        // Toggle group para estado
        ToggleGroup tg = new ToggleGroup();
        chipTodos.setToggleGroup(tg);
        chipActivo.setToggleGroup(tg);
        chipPasivo.setToggleGroup(tg);
        chipTodos.setSelected(true);
        tg.selectedToggleProperty().addListener((obs, o, n) -> aplicarFiltros());

        // ComboBoxes
        comboServicio.getItems().add("Todos");
        comboServicio.getItems().addAll(service.getServicios());
        comboServicio.setValue("Todos");
        comboServicio.setOnAction(e -> aplicarFiltros());

        comboDelegacion.getItems().add("Todas");
        comboDelegacion.getItems().addAll(service.getDelegaciones());
        comboDelegacion.setValue("Todas");
        comboDelegacion.setOnAction(e -> aplicarFiltros());

        comboAgente.getItems().add("Todos");
        comboAgente.getItems().addAll(service.getAgentes());
        comboAgente.setValue("Todos");
        comboAgente.setOnAction(e -> aplicarFiltros());

        // Búsqueda en tiempo real
        searchField.textProperty().addListener((obs, o, n) -> aplicarFiltros());
    }

    // ─── Carga de datos ───────────────────────────────────────────────────────

    private void cargarDatos() {
        masterList.setAll(service.obtenerTodas());
        filteredList = new FilteredList<>(masterList, p -> true);
        SortedList<Empresa> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(tablaEmpresas.comparatorProperty());
        tablaEmpresas.setItems(sorted);
        actualizarEstadoVacio();
    }

    private void aplicarFiltros() {
        if (filteredList == null) return;
        filteredList.setPredicate(e -> {
            // Texto de búsqueda
            String q = searchField.getText().toLowerCase();
            boolean matchQ = q.isBlank() ||
                safe(e.getDenominacionSocial()).contains(q) ||
                safe(e.getNifCif()).contains(q) ||
                safe(e.getAbreviatura()).contains(q);

            // Estado
            ToggleButton sel = (ToggleButton) chipTodos.getToggleGroup().getSelectedToggle();
            boolean matchE = sel == null || sel == chipTodos ||
                (sel == chipActivo && e.isActivo()) ||
                (sel == chipPasivo && !e.isActivo());

            // Servicio
            String sv = comboServicio.getValue();
            boolean matchS = sv == null || sv.equals("Todos") || sv.equals(e.getServicio());

            // Delegación
            String dv = comboDelegacion.getValue();
            boolean matchD = dv == null || dv.equals("Todas") || dv.equals(e.getDelegacion());

            // Agente
            String av = comboAgente.getValue();
            boolean matchA = av == null || av.equals("Todos") || av.equals(e.getAgenteContable());

            return matchQ && matchE && matchS && matchD && matchA;
        });
        actualizarEstadoVacio();
    }

    private void actualizarEstadoVacio() {
        boolean vacio = tablaEmpresas.getItems().isEmpty();
        tablaEmpresas.setVisible(!vacio);
        lblEmpty.setVisible(vacio);
    }

    private String safe(String s) { return s != null ? s.toLowerCase() : ""; }

    // ─── Acciones de usuario ─────────────────────────────────────────────────

    @FXML private void onNuevaEmpresa() {
        abrirFormulario(null);
    }

    private void abrirFormulario(Empresa empresaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/controltotal_proyecto/fxml/dialogs/EmpresaFormDialog.fxml")
            );
            Parent root = loader.load();
            EmpresaFormController ctrl = loader.getController();
            ctrl.init(empresaEditar, result -> {
                cargarDatos();
                if (mainController != null) mainController.refreshStats();
            });

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(empresaEditar == null ? "Nueva Empresa" : "Editar Empresa");
            dialog.setScene(new Scene(root));
            dialog.getScene().getStylesheets().add(
                getClass().getResource("/com/example/controltotal_proyecto/css/styles.css").toExternalForm()
            );
            dialog.setMinWidth(740);
            dialog.setMinHeight(650);
            dialog.show();

        } catch (IOException ex) {
            mostrarError("No se pudo abrir el formulario de empresa.", ex);
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

    private void mostrarError(String msg, Exception ex) {
        ex.printStackTrace();
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}
