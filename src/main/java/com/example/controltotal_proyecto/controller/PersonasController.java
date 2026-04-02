package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la vista "Personas".
 */
public class PersonasController implements Initializable,
    MainController.RefreshableController, MainController.ChildController {

    // ─── Tabla ────────────────────────────────────────────────────────────────
    @FXML private TableView<Persona>          tablaPersonas;
    @FXML private TableColumn<Persona,String> colNombre;
    @FXML private TableColumn<Persona,String> colApellidos;
    @FXML private TableColumn<Persona,String> colNif;
    @FXML private TableColumn<Persona,String> colMovil;
    @FXML private TableColumn<Persona,String> colEmail;
    @FXML private TableColumn<Persona,Void>   colEmpresas;
    @FXML private TableColumn<Persona,String> colEstado;
    @FXML private TableColumn<Persona,Void>   colAcciones;

    // ─── Filtros ──────────────────────────────────────────────────────────────
    @FXML private TextField    searchField;
    @FXML private ToggleButton chipTodos;
    @FXML private ToggleButton chipActivo;
    @FXML private ToggleButton chipInactivo;

    @FXML private VBox lblEmpty;

    // ─── Datos ───────────────────────────────────────────────────────────────
    private final PersonaService            service    = new PersonaService();
    private final ObservableList<Persona>   masterList = FXCollections.observableArrayList();
    private       FilteredList<Persona>     filtered;
    private       MainController            mainController;

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();
        configurarFiltros();
        cargarDatos();
    }

    @Override public void onViewActivated()                      { cargarDatos(); }
    @Override public void setMainController(MainController main) { this.mainController = main; }

    // ─── Columnas ─────────────────────────────────────────────────────────────

    private void configurarColumnas() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colNif.setCellValueFactory(new PropertyValueFactory<>("nif"));
        colMovil.setCellValueFactory(new PropertyValueFactory<>("contactoMovil"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("contactoMail"));

        // Empresas relacionadas — lista de tags
        colEmpresas.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Persona p = getTableView().getItems().get(getIndex());
                FlowPane flow = new FlowPane(4, 4);
                List<String> nifs = DatabaseManager.obtenerEmpresasDePersona(p.getNif());
                if (nifs.isEmpty()) {
                    Label lbl = new Label("Sin empresas");
                    lbl.getStyleClass().add("text-muted");
                    flow.getChildren().add(lbl);
                } else {
                    nifs.forEach(nif -> {
                        Empresa e = DatabaseManager.obtenerEmpresaPorNif(nif);
                        if (e != null) flow.getChildren().add(BadgeFactory.empresaTag(e.getAbreviatura()));
                    });
                }
                setGraphic(flow);
            }
        });

        // Estado — badge
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                setGraphic(BadgeFactory.estadoBadge(v)); setText(null);
            }
        });

        // Acciones
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar  = crearBtn("Editar", "btn-icon-edit");
            private final Button btnCarpeta = crearBtn("Carpeta", "btn-icon-folder");
            private final HBox   box        = new HBox(6, btnEditar, btnCarpeta);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnCarpeta.setOnAction(e -> {
                    Persona p = getTableView().getItems().get(getIndex());
                    abrirCarpeta(p.getRutaDocumental());
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablaPersonas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private Button crearBtn(String tooltip, String styleClass) {
        Button btn = new Button();
        btn.setTooltip(new Tooltip(tooltip));
        btn.getStyleClass().addAll("btn-icon", styleClass);
        return btn;
    }

    // ─── Filtros ──────────────────────────────────────────────────────────────

    private void configurarFiltros() {
        ToggleGroup tg = new ToggleGroup();
        chipTodos.setToggleGroup(tg);
        chipActivo.setToggleGroup(tg);
        chipInactivo.setToggleGroup(tg);
        chipTodos.setSelected(true);
        tg.selectedToggleProperty().addListener((obs, o, n) -> aplicarFiltros());
        searchField.textProperty().addListener((obs, o, n) -> aplicarFiltros());
    }

    private void cargarDatos() {
        masterList.setAll(service.obtenerTodas());
        filtered = new FilteredList<>(masterList, p -> true);
        SortedList<Persona> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tablaPersonas.comparatorProperty());
        tablaPersonas.setItems(sorted);
        actualizarVacio();
    }

    private void aplicarFiltros() {
        if (filtered == null) return;
        filtered.setPredicate(p -> {
            String q = searchField.getText().toLowerCase();
            boolean mQ = q.isBlank() ||
                p.getNombreCompleto().toLowerCase().contains(q) ||
                safe(p.getNif()).contains(q) ||
                safe(p.getContactoMail()).contains(q);

            ToggleButton sel = (ToggleButton) chipTodos.getToggleGroup().getSelectedToggle();
            boolean mE = sel == null || sel == chipTodos ||
                (sel == chipActivo && p.isActivo()) ||
                (sel == chipInactivo && !p.isActivo());

            return mQ && mE;
        });
        actualizarVacio();
    }

    private void actualizarVacio() {
        boolean vacio = tablaPersonas.getItems().isEmpty();
        tablaPersonas.setVisible(!vacio);
        lblEmpty.setVisible(vacio);
    }

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
