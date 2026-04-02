package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.service.EmpresaService;
import com.example.controltotal_proyecto.service.PersonaService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controlador del diálogo de creación/edición de Empresa.
 * Recibe una empresa para edición (o null para nueva) y devuelve el resultado
 * mediante un callback Consumer<Empresa>.
 */
public class EmpresaFormController implements Initializable {

    // ─── Empresa ──────────────────────────────────────────────────────────────
    @FXML private TextField   txtNifCif;
    @FXML private ComboBox<String> comboFormaSocial;
    @FXML private TextField   txtDenominacion;
    @FXML private TextField   txtAbreviatura;
    @FXML private Label       lblAbrPreview;
    @FXML private Label       lblRutaPreview;
    @FXML private ComboBox<String> comboServicio;
    @FXML private ComboBox<String> comboDelegacion;
    @FXML private ComboBox<String> comboAgente;
    @FXML private ComboBox<String> comboEstado;

    // ─── Primer contacto ──────────────────────────────────────────────────────
    @FXML private TextField   txtCNombre;
    @FXML private TextField   txtCApellidos;
    @FXML private TextField   txtCNif;
    @FXML private TextField   txtCMovil;
    @FXML private TextField   txtCEmail;

    // ─── Personas relacionadas adicionales ────────────────────────────────────
    @FXML private VBox        vboxPersonas;
    @FXML private TextField   txtBuscarPersona;
    @FXML private VBox        vboxSugerencias;

    // ─── Servicios ────────────────────────────────────────────────────────────
    private final EmpresaService empService = new EmpresaService();
    private final PersonaService perService = new PersonaService();

    // ─── Estado interno ───────────────────────────────────────────────────────
    private Empresa           empresaEditar;
    private Consumer<Empresa> callback;
    private final List<Persona> personasSeleccionadas = new ArrayList<>();

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Poblar combos
        comboFormaSocial.getItems().addAll("SL","SLL","SA","SAL","SDAD. COOP. AND.","CB");
        comboServicio.getItems().addAll(empService.getServicios());
        comboDelegacion.getItems().addAll(empService.getDelegaciones());
        comboAgente.getItems().addAll(empService.getAgentes());
        comboEstado.getItems().addAll("Activo","Pasivo");
        comboEstado.setValue("Activo");

        // Auto-generar abreviatura al escribir la denominación
        txtDenominacion.textProperty().addListener((obs, o, n) -> {
            String abr = empService.generarAbreviatura(n);
            if (empresaEditar == null) txtAbreviatura.setText(abr);
            actualizarPreviews();
        });
        txtAbreviatura.textProperty().addListener((obs, o, n) -> actualizarPreviews());

        // Búsqueda de personas adicionales
        txtBuscarPersona.textProperty().addListener((obs, o, n) -> renderSugerencias(n));
    }

    /**
     * Inicializa el formulario con una empresa existente (edición) o null (nueva).
     * @param empresa  null = nueva; not null = editar
     * @param callback llamado con la empresa guardada al confirmar
     */
    public void init(Empresa empresa, Consumer<Empresa> callback) {
        this.empresaEditar = empresa;
        this.callback      = callback;

        if (empresa != null) {
            txtNifCif.setText(empresa.getNifCif());
            txtNifCif.setDisable(true); // NIF no editable en edición
            comboFormaSocial.setValue(empresa.getFormaSocial());
            txtDenominacion.setText(empresa.getDenominacionSocial());
            txtAbreviatura.setText(empresa.getAbreviatura());
            comboServicio.setValue(empresa.getServicio());
            comboDelegacion.setValue(empresa.getDelegacion());
            comboAgente.setValue(empresa.getAgenteContable());
            comboEstado.setValue(empresa.isActivo() ? "Activo" : "Pasivo");

            // Cargar personas vinculadas
            List<String> nifs = DatabaseManager.obtenerPersonasDeEmpresa(empresa.getNifCif());
            nifs.forEach(nif -> {
                Persona p = DatabaseManager.obtenerPersonaPorNif(nif);
                if (p != null) {
                    personasSeleccionadas.add(p);
                    agregarTarjetaPersona(p);
                }
            });
        }
        actualizarPreviews();
    }

    // ─── Previsualización ─────────────────────────────────────────────────────

    private void actualizarPreviews() {
        String abr = txtAbreviatura.getText().isBlank()
            ? empService.generarAbreviatura(txtDenominacion.getText())
            : txtAbreviatura.getText();
        lblAbrPreview.setText(abr.isBlank() ? "—" : abr);
        lblRutaPreview.setText(abr.isBlank() ? "—" : "servidor/empresas/" + abr + "/");
    }

    // ─── Personas relacionadas ────────────────────────────────────────────────

    private void renderSugerencias(String query) {
        vboxSugerencias.getChildren().clear();
        if (query == null || query.isBlank()) return;

        String q = query.toLowerCase();
        perService.obtenerTodas().stream()
            .filter(p -> personasSeleccionadas.stream()
                .noneMatch(s -> s.getNif().equals(p.getNif())))
            .filter(p -> p.getNombreCompleto().toLowerCase().contains(q)
                      || p.getNif().toLowerCase().contains(q))
            .limit(6)
            .forEach(p -> {
                Label lbl = new Label(p.getNombreCompleto() + " · " + p.getNif());
                lbl.getStyleClass().add("sugerencia-item");
                lbl.setOnMouseClicked(e -> {
                    personasSeleccionadas.add(p);
                    agregarTarjetaPersona(p);
                    txtBuscarPersona.clear();
                    vboxSugerencias.getChildren().clear();
                });
                vboxSugerencias.getChildren().add(lbl);
            });
    }

    private void agregarTarjetaPersona(Persona p) {
        HBox card = new HBox(10);
        card.getStyleClass().add("person-card");
        card.setPadding(new Insets(8, 12, 8, 12));

        Label avatar = new Label(
            String.valueOf(p.getNombre().charAt(0)) + p.getApellidos().charAt(0)
        );
        avatar.getStyleClass().add("person-avatar");

        VBox info = new VBox(2);
        Label nombre = new Label(p.getNombreCompleto());
        nombre.getStyleClass().add("person-name");
        Label meta = new Label(p.getNif() + " · " + p.getContactoMovil() + " · " + p.getContactoMail());
        meta.getStyleClass().add("person-meta");
        info.getChildren().addAll(nombre, meta);

        Button btnRemove = new Button("✕");
        btnRemove.getStyleClass().add("btn-remove-person");
        btnRemove.setOnAction(e -> {
            personasSeleccionadas.remove(p);
            vboxPersonas.getChildren().remove(card);
        });

        card.getChildren().addAll(avatar, info, btnRemove);
        vboxPersonas.getChildren().add(card);
    }

    // ─── Guardar ─────────────────────────────────────────────────────────────

    @FXML private void onGuardar() {
        if (!validar()) return;

        boolean esActivo = "Activo".equals(comboEstado.getValue());

        Empresa e = empresaEditar != null ? empresaEditar : new Empresa();
        e.setNifCif(txtNifCif.getText().trim());
        e.setDenominacionSocial(txtDenominacion.getText().trim());
        e.setFormaSocial(comboFormaSocial.getValue());
        e.setActivo(esActivo);
        e.setAbreviatura(txtAbreviatura.getText().isBlank()
            ? empService.generarAbreviatura(e.getDenominacionSocial())
            : txtAbreviatura.getText().trim());
        e.setServicio(comboServicio.getValue());
        e.setDelegacion(comboDelegacion.getValue());
        e.setAgenteContable(comboAgente.getValue());

        // Construir primer contacto desde los campos del formulario
        Persona primerContacto = null;
        if (!txtCNif.getText().isBlank() && !txtCNombre.getText().isBlank()) {
            primerContacto = new Persona();
            primerContacto.setNif(txtCNif.getText().trim());
            primerContacto.setNombre(txtCNombre.getText().trim());
            primerContacto.setApellidos(txtCApellidos.getText().trim());
            primerContacto.setContactoMovil(txtCMovil.getText().trim());
            primerContacto.setContactoMail(txtCEmail.getText().trim());
            primerContacto.setActivo(true);
        }

        boolean ok;
        if (empresaEditar == null) {
            ok = empService.crear(e, primerContacto);
        } else {
            ok = empService.actualizar(e);
            // Sincronizar personas adicionales
            List<String> actualNifs = DatabaseManager.obtenerPersonasDeEmpresa(e.getNifCif());
            for (Persona p : personasSeleccionadas) {
                if (!actualNifs.contains(p.getNif())) {
                    DatabaseManager.vincularEmpresaPersona(e.getNifCif(), p.getNif());
                }
            }
        }

        if (ok) {
            if (callback != null) callback.accept(e);
            cerrar();
        } else {
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar la empresa. Revisa los datos.").show();
        }
    }

    @FXML private void onCancelar() { cerrar(); }

    private boolean validar() {
        if (txtNifCif.getText().isBlank()) { mostrarReq("NIF/CIF"); return false; }
        if (txtDenominacion.getText().isBlank()) { mostrarReq("Denominación Social"); return false; }
        if (comboFormaSocial.getValue() == null) { mostrarReq("Forma Social"); return false; }
        if (comboServicio.getValue() == null) { mostrarReq("Servicio"); return false; }
        if (comboDelegacion.getValue() == null) { mostrarReq("Delegación"); return false; }
        if (comboAgente.getValue() == null) { mostrarReq("Agente Contable"); return false; }
        return true;
    }

    private void mostrarReq(String campo) {
        new Alert(Alert.AlertType.WARNING, "El campo \"" + campo + "\" es obligatorio.").show();
    }

    private void cerrar() {
        ((Stage) txtNifCif.getScene().getWindow()).close();
    }
}
