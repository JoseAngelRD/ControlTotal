package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.service.EmpresaService;
import com.example.controltotal_proyecto.service.PersonaService;
import com.example.controltotal_proyecto.util.AlertaUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controlador del formulario de creación/edición de Empresa.
 *
 * CAMBIOS respecto a la versión anterior:
 *  - Validación de formato CIF: Letra + 8 dígitos (ej: B12345678).
 *  - El campo Abreviatura es obligatorio (no se auto-genera si está vacío al guardar).
 *  - Los ComboBoxes del formulario tienen ancho fijo (no crecen al seleccionar).
 *  - Las alertas de error muestran el estilo personalizado (fondo azul, icono amarillo).
 *  - Ya NO cierra ningún Stage. En su lugar llama a {@link #closeAction}.
 */
public class EmpresaFormController implements Initializable {

    // ─── Empresa ──────────────────────────────────────────────────────────────
    @FXML private TextField        txtNifCif;
    @FXML private ComboBox<String> comboFormaSocial;
    @FXML private TextField        txtDenominacion;
    @FXML private TextField        txtAbreviatura;
    @FXML private Label            lblAbrPreview;
    @FXML private Label            lblRutaPreview;
    @FXML private ComboBox<String> comboServicio;
    @FXML private ComboBox<String> comboDelegacion;
    @FXML private ComboBox<String> comboAgente;
    @FXML private ComboBox<String> comboEstado;

    // ─── Primer contacto ──────────────────────────────────────────────────────
    @FXML private TextField txtCNombre;
    @FXML private TextField txtCApellidos;
    @FXML private TextField txtCNif;
    @FXML private TextField txtCMovil;
    @FXML private TextField txtCEmail;

    // ─── Personas relacionadas ────────────────────────────────────────────────
    @FXML private VBox      vboxPersonas;
    @FXML private TextField txtBuscarPersona;
    @FXML private VBox      vboxSugerencias;

    // ─── Servicios ────────────────────────────────────────────────────────────
    private final EmpresaService empService = new EmpresaService();
    private final PersonaService perService = new PersonaService();

    // ─── Estado interno ───────────────────────────────────────────────────────
    private Empresa           empresaEditar;
    private Consumer<Empresa> callback;
    private Runnable          closeAction;
    private final List<Persona> personasSeleccionadas = new ArrayList<>();

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboFormaSocial.getItems().addAll(
            "SL.", "SLL.", "SLP.", "SA.", "SAL.",
            "Sdad.Coop.And.", "PF.", "Fund.", "Asoc.", "SC.", "SCP.", "UTE."
        );
        comboServicio.getItems().addAll(empService.getServicios());
        comboDelegacion.getItems().addAll(empService.getDelegaciones());
        comboAgente.getItems().addAll(empService.getAgentes());
        comboEstado.getItems().addAll("Activo", "Pasivo");
        comboEstado.setValue("Activo");

        // Fijar ancho de los ComboBoxes para que no crezcan al seleccionar
        fijarAnchoCombo(comboFormaSocial);
        fijarAnchoCombo(comboServicio);
        fijarAnchoCombo(comboDelegacion);
        fijarAnchoCombo(comboAgente);
        fijarAnchoCombo(comboEstado);

        txtDenominacion.textProperty().addListener((obs, o, n) -> {
            // Auto-generar abreviatura SÓLO al crear (no al editar)
            if (empresaEditar == null && txtAbreviatura.getText().isBlank()) {
                txtAbreviatura.setText(empService.generarAbreviatura(n));
            }
            actualizarPreviews();
            actualizarRuta();
        });
        txtAbreviatura.textProperty().addListener((obs, o, n) -> actualizarPreviews());
        comboEstado.valueProperty().addListener((obs, o, n) -> actualizarRuta());
        comboFormaSocial.valueProperty().addListener((obs, o, n) -> actualizarRuta());

        txtBuscarPersona.textProperty().addListener((obs, o, n) -> renderSugerencias(n));

        actualizarRuta();
    }

    /**
     * Fija el ancho preferido de un ComboBox al valor calculado tras añadir los items,
     * evitando que crezca al seleccionar una opción más larga.
     */
    private void fijarAnchoCombo(ComboBox<?> combo) {
        combo.setMaxWidth(Double.MAX_VALUE);
        // Se marca para que el HBox padre controle el ancho mediante HGROW
        combo.setPrefWidth(0);
        combo.setMinWidth(80);
    }

    /** Registra el Runnable que vuelve a la vista lista al cancelar / guardar. */
    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    /**
     * Inicializa el formulario con una empresa existente (edición) o null (nueva).
     */
    public void init(Empresa empresa, Consumer<Empresa> callback) {
        this.empresaEditar = empresa;
        this.callback      = callback;

        if (empresa != null) {
            txtNifCif.setText(empresa.getNifCif());
            txtNifCif.setDisable(true);
            comboFormaSocial.setValue(empresa.getFormaSocial());
            txtDenominacion.setText(empresa.getDenominacionSocial());
            txtAbreviatura.setText(empresa.getAbreviatura());
            comboServicio.setValue(empresa.getServicio());
            comboDelegacion.setValue(empresa.getDelegacion());
            comboAgente.setValue(empresa.getAgenteContable());
            comboEstado.setValue(empresa.isActivo() ? "Activo" : "Pasivo");

            List<Persona> personasVinculadas =
                    DatabaseManager.obtenerPersonasDeEmpresa(empresa.getNifCif());
            personasVinculadas.forEach(p -> {
                if (p != null) {
                    personasSeleccionadas.add(p);
                    agregarTarjetaPersona(p);
                }
            });
        }
        actualizarPreviews();
        actualizarRuta();
    }

    // ─── Previsualización ─────────────────────────────────────────────────────

    private void actualizarPreviews() {
        String abr = txtAbreviatura.getText().isBlank()
                ? empService.generarAbreviatura(txtDenominacion.getText())
                : txtAbreviatura.getText();
        lblAbrPreview.setText(abr.isBlank() ? "—" : abr);
    }

    private void actualizarRuta() {
        String estado      = comboEstado.getValue();
        String nombre      = txtDenominacion.getText().trim();
        String formaSocial = comboFormaSocial.getValue();

        if (nombre.isBlank()) {
            lblRutaPreview.setText("Y:/DocumOfi/" + estado + "/Empresas");
        } else {
            String sufijoForma = (formaSocial != null && !formaSocial.isBlank())
                    ? " " + formaSocial : "";
            lblRutaPreview.setText(
                    "Y:/DocumOfi/" + estado + "/Empresas/" + nombre + sufijoForma + "/");
        }
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
                String.valueOf(p.getNombre().charAt(0)) + p.getApellidos().charAt(0));
        avatar.getStyleClass().add("person-avatar");

        VBox info = new VBox(2);
        Label nombre = new Label(p.getNombreCompleto());
        nombre.getStyleClass().add("person-name");
        Label meta = new Label(
                p.getNif() + " · " + p.getContactoMovil() + " · " + p.getContactoMail());
        meta.getStyleClass().add("person-meta");
        info.getChildren().addAll(nombre, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAuto = new Button("⭐ Principal");
        btnAuto.getStyleClass().add("btn-ghost");
        btnAuto.setOnAction(e -> {
            txtCNombre.setText(p.getNombre()        != null ? p.getNombre()        : "");
            txtCApellidos.setText(p.getApellidos()  != null ? p.getApellidos()     : "");
            txtCNif.setText(p.getNif()              != null ? p.getNif()           : "");
            txtCMovil.setText(p.getContactoMovil()  != null ? p.getContactoMovil() : "");
            txtCEmail.setText(p.getContactoMail()   != null ? p.getContactoMail()  : "");
        });

        Button btnRemove = new Button("✕");
        btnRemove.getStyleClass().add("btn-remove-person");
        btnRemove.setOnAction(e -> {
            personasSeleccionadas.remove(p);
            vboxPersonas.getChildren().remove(card);
        });

        card.getChildren().addAll(avatar, info, spacer, btnAuto, btnRemove);
        vboxPersonas.getChildren().add(card);
    }

    // ─── Guardar ──────────────────────────────────────────────────────────────

    @FXML private void onGuardar() {
        if (!validar()) return;

        boolean esActivo = "Activo".equals(comboEstado.getValue());

        Empresa e = empresaEditar != null ? empresaEditar : new Empresa();
        e.setNifCif(txtNifCif.getText().trim());
        e.setDenominacionSocial(txtDenominacion.getText().trim());
        e.setFormaSocial(comboFormaSocial.getValue());
        e.setActivo(esActivo);
        // Abreviatura ya validada como no vacía → usar directamente
        e.setAbreviatura(txtAbreviatura.getText().trim());
        e.setServicio(comboServicio.getValue());
        e.setDelegacion(comboDelegacion.getValue());
        e.setAgenteContable(comboAgente.getValue());

        String nombreCompleto = (txtCNombre.getText().trim()
                + " " + txtCApellidos.getText().trim()).trim();
        e.setContactoNombre(nombreCompleto);
        e.setContactoMovil(txtCMovil.getText().trim());
        e.setContactoMail(txtCEmail.getText().trim());
        e.setContactoDNI(txtCNif.getText().trim());

        boolean ok;
        if (empresaEditar == null) {
            ok = empService.crear(e, null);
            if (ok) {
                for (Persona p : personasSeleccionadas) {
                    DatabaseManager.vincularEmpresaPersona(e.getNifCif(), p.getNif());
                }
            }
        } else {
            ok = empService.actualizar(e);
            if (ok) {
                List<Persona> actualesBD =
                        DatabaseManager.obtenerPersonasDeEmpresa(e.getNifCif());
                List<String> nifsActuales =
                        actualesBD.stream().map(Persona::getNif).toList();
                for (Persona p : personasSeleccionadas) {
                    if (!nifsActuales.contains(p.getNif())) {
                        DatabaseManager.vincularEmpresaPersona(e.getNifCif(), p.getNif());
                    }
                }
            }
        }

        if (ok) {
            if (callback != null) callback.accept(e);
            cerrar();
        } else {
            mostrarAlerta("No se pudo guardar la empresa. Revisa los datos.");
        }
    }

    @FXML private void onCancelar() { cerrar(); }

    private void cerrar() {
        if (closeAction != null) {
            closeAction.run();
        } else {
            ((javafx.stage.Stage) txtNifCif.getScene().getWindow()).close();
        }
    }

    // ─── Validación ───────────────────────────────────────────────────────────

    private boolean validar() {
        // NIF/CIF: obligatorio
        String cif = txtNifCif.getText().trim();
        if (cif.isBlank()) {
            mostrarReq("NIF/CIF");
            return false;
        }
        // NIF/CIF: patrón letra + 8 dígitos
        if (!cif.matches("[A-Za-z]\\d{8}")) {
            mostrarAlerta("El formato del NIF/CIF no es válido.\nDebe ser: 1 letra + 8 dígitos\n(ej: B12345678).");
            return false;
        }

        if (txtDenominacion.getText().isBlank())  { mostrarReq("Denominación Social"); return false; }
        if (comboFormaSocial.getValue() == null)  { mostrarReq("Forma Social");        return false; }

        // Abreviatura: obligatoria, no se auto-genera si está vacía
        if (txtAbreviatura.getText().isBlank())   { mostrarReq("Abreviatura");          return false; }

        if (comboServicio.getValue()   == null)   { mostrarReq("Servicio");             return false; }
        if (comboDelegacion.getValue() == null)   { mostrarReq("Delegación");           return false; }
        if (comboAgente.getValue()     == null)   { mostrarReq("Agente Contable");      return false; }

        return true;
    }

    private void mostrarReq(String campo) {
        mostrarAlerta("El campo \"" + campo + "\" es obligatorio.");
    }

    private void mostrarAlerta(String mensaje) {
        Window owner = txtNifCif.getScene() != null
                ? txtNifCif.getScene().getWindow()
                : null;
        AlertaUtil.mostrarAdvertencia(mensaje, owner);
    }
}
