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

/**
 * Controlador de la vista "Buscador Avanzado".
 * Permite buscar empresas y personas de forma combinada.
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
    @FXML private VBox             resultContainer;

    private MainController   mainController;
    private final EmpresaService empService = new EmpresaService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ToggleGroup tg = new ToggleGroup();
        chipTodos.setToggleGroup(tg);
        chipEmpresas.setToggleGroup(tg);
        chipPersonas.setToggleGroup(tg);
        chipTodos.setSelected(true);

        comboServicio.getItems().add("Todos");
        comboServicio.getItems().addAll(empService.getServicios());
        comboServicio.setValue("Todos");

        comboDelegacion.getItems().add("Todas");
        comboDelegacion.getItems().addAll(empService.getDelegaciones());
        comboDelegacion.setValue("Todas");

        comboEstado.getItems().addAll("Cualquier estado","Activo","Pasivo");
        comboEstado.setValue("Cualquier estado");

        // Listeners para búsqueda reactiva
        searchField.textProperty().addListener((obs, o, n) -> buscar());
        tg.selectedToggleProperty().addListener((obs, o, n) -> buscar());
        comboServicio.setOnAction(e -> buscar());
        comboDelegacion.setOnAction(e -> buscar());
        comboEstado.setOnAction(e -> buscar());

        mostrarPlaceholder();
    }

    @Override public void onViewActivated()                      { buscar(); }
    @Override public void setMainController(MainController main) { this.mainController = main; }

    @FXML private void onBuscar() { buscar(); }

    // ─── Lógica de búsqueda ───────────────────────────────────────────────────

    private void buscar() {
        String q    = searchField.getText().toLowerCase().trim();
        String tipo = chipEmpresas.isSelected() ? "empresa"
                    : chipPersonas.isSelected() ? "persona" : "";
        String sv   = comboServicio.getValue();
        String dv   = comboDelegacion.getValue();
        String ev   = comboEstado.getValue();

        if (q.isBlank() && tipo.isBlank() && "Todos".equals(sv)
                && "Todas".equals(dv) && "Cualquier estado".equals(ev)) {
            mostrarPlaceholder();
            return;
        }

        List<Object[]> resultados = new ArrayList<>(); // [tipo, objeto]

        if (tipo.isEmpty() || tipo.equals("empresa")) {
            DatabaseManager.obtenerTodasLasEmpresas().stream()
                .filter(e -> q.isBlank()
                    || safe(e.getDenominacionSocial()).contains(q)
                    || safe(e.getNifCif()).contains(q)
                    || safe(e.getAbreviatura()).contains(q))
                .filter(e -> "Todos".equals(sv) || sv.equals(e.getServicio()))
                .filter(e -> "Todas".equals(dv) || dv.equals(e.getDelegacion()))
                .filter(e -> "Cualquier estado".equals(ev)
                    || ("Activo".equals(ev) && e.isActivo())
                    || ("Pasivo".equals(ev) && !e.isActivo()))
                .forEach(e -> resultados.add(new Object[]{"empresa", e}));
        }

        if (tipo.isEmpty() || tipo.equals("persona")) {
            DatabaseManager.obtenerTodasLasPersonas().stream()
                .filter(p -> q.isBlank()
                    || p.getNombreCompleto().toLowerCase().contains(q)
                    || safe(p.getNif()).contains(q)
                    || safe(p.getContactoMail()).contains(q))
                .filter(p -> "Cualquier estado".equals(ev)
                    || ("Activo".equals(ev) && p.isActivo())
                    || ("Pasivo".equals(ev) && !p.isActivo()))
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

        // Cabecera
        Label header = new Label(resultados.size() + " resultado" + (resultados.size() > 1 ? "s" : ""));
        header.getStyleClass().add("results-count");
        resultContainer.getChildren().add(header);

        // Filas de resultado
        for (Object[] row : resultados) {
            resultContainer.getChildren().add(
                "empresa".equals(row[0])
                    ? crearFilaEmpresa((Empresa) row[1])
                    : crearFilaPersona((Persona) row[1])
            );
        }
    }

    private HBox crearFilaEmpresa(Empresa e) {
        Label tipo = new Label("Empresa");
        tipo.getStyleClass().addAll("badge-service", "badge-service-" + safe(e.getServicio()));
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
        Label info = new Label(e.getServicio() + " · " + e.getDelegacion());
        info.getStyleClass().add("result-meta");
        Label est  = BadgeFactory.estadoBadge(e.getEstado());

        Button btnEdit = crearBtn("btn-icon-edit");
        btnEdit.setOnAction(ev -> abrirFormEmpresa(e));

        HBox row = new HBox(10, tipo, nombreBox, nif, info, est, btnEdit);
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

        Button btnEdit = crearBtn("btn-icon-edit");
        btnEdit.setOnAction(ev -> abrirFormPersona(p));

        HBox row = new HBox(10, tipo, nombre, nif, info, est, btnEdit);
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
                getClass().getResource("/com/example/controltotal_proyecto/fxml/dialogs/EmpresaFormDialog.fxml")
            );
            Parent root = loader.load();
            EmpresaFormController ctrl = loader.getController();
            ctrl.init(e, result -> buscar());
            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Editar Empresa");
            st.setScene(new Scene(root));
            st.getScene().getStylesheets().add(getClass().getResource("/com/example/controltotal_proyecto/css/styles.css").toExternalForm());
            st.show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void abrirFormPersona(Persona p) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/controltotal_proyecto/fxml/dialogs/PersonaFormDialog.fxml")
            );
            Parent root = loader.load();
            PersonaFormController ctrl = loader.getController();
            ctrl.init(p, result -> buscar());
            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Editar Persona");
            st.setScene(new Scene(root));
            st.getScene().getStylesheets().add(getClass().getResource("/com/example/controltotal_proyecto/css/styles.css").toExternalForm());
            st.show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }
}
