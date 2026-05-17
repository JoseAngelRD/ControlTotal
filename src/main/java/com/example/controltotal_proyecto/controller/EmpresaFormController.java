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

//mover carpetas de pasivo a activo, o cambiar nombre
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.Objects;

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

    private String inicioRuta = "C:/Control_Total/";   //  Y:/DocumOfi/

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

    //Interfaz formulario
    @FXML private Label dialogTitle;

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
            //contacto
            txtCNombre.setText(empresa.getContactoNombre());
            txtCNif.setText(empresa.getContactoDNI());
            txtCMovil.setText(empresa.getContactoMovil());
            txtCEmail.setText(empresa.getContactoMail());

            List<Persona> personasVinculadas =
                    DatabaseManager.obtenerPersonasDeEmpresa(empresa.getNifCif());
            personasVinculadas.forEach(p -> {
                if (p != null) {
                    personasSeleccionadas.add(p);
                    agregarTarjetaPersona(p);
                }
            });

            dialogTitle.setText("Modificar Empresa");
        } else {
            dialogTitle.setText("Nueva Empresa");
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

    private String construirRuta(boolean isActivo, String denominacion, String formaSocial) {
        String estado = isActivo ? "Activo" : "Pasivo";
        String sufijoForma = (formaSocial != null && !formaSocial.isBlank()) ? " " + formaSocial : "";
        return inicioRuta + estado + "/Empresas/" + denominacion + sufijoForma;
    }

    private void actualizarRuta() {
        String nombre = txtDenominacion.getText().trim();
        if (nombre.isBlank()) {
            lblRutaPreview.setText(inicioRuta + comboEstado.getValue() + "/Empresas");
        } else {
            lblRutaPreview.setText(construirRuta("Activo".equals(comboEstado.getValue()), nombre, comboFormaSocial.getValue()) + "/");
        }
    }

    private void moverCarpetaFisica(String rutaOrigen, String rutaDestino) {
        Path origen = Paths.get(rutaOrigen);
        Path destino = Paths.get(rutaDestino);
        if (Files.exists(origen) && !rutaOrigen.equals(rutaDestino)) {
            try {
                // Aseguramos que la carpeta padre (Activo/Empresas o Pasivo/Empresas) exista
                Files.createDirectories(destino.getParent());
                // Movemos la carpeta
                Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
                mostrarAlerta("Se guardó la empresa en base de datos, pero hubo un error al mover la carpeta de archivos.\n" +
                        "Por favor, muévela manualmente de:\n" + rutaOrigen + "\na:\n" + rutaDestino);
            }
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
            txtCNombre.setText(p.getNombre()        != null &&  p.getApellidos()  != null? p.getNombre() + " " + p.getApellidos()        : "");
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
        String nombreNuevo = txtDenominacion.getText().trim();
        String formaNueva = comboFormaSocial.getValue();

        // --- 1. DETECTAR CAMBIOS DE RUTA (Antes de modificar el objeto) ---
        boolean requiereMoverCarpeta = false;
        String rutaAntigua = null;
        String rutaNueva = null;

        Empresa e = empresaEditar != null ? empresaEditar : new Empresa();

        if (empresaEditar != null) {
            String nombreViejo = empresaEditar.getDenominacionSocial();
            String formaVieja = empresaEditar.getFormaSocial();
            boolean estadoViejo = empresaEditar.isActivo();

            // Si cambia el estado, el nombre o la forma social, la ruta de la carpeta cambia.
            if (estadoViejo != esActivo || !nombreViejo.equals(nombreNuevo) || !Objects.equals(formaVieja, formaNueva)) {
                rutaAntigua = construirRuta(estadoViejo, nombreViejo, formaVieja);
                rutaNueva = construirRuta(esActivo, nombreNuevo, formaNueva);
                requiereMoverCarpeta = true;

                //actualizo a rutas nuevas
                String rutaNuevaCopia = rutaNueva;

                rutaNuevaCopia = rutaNuevaCopia.replace("/", "\\");
                e.setRutaDocumental(rutaNuevaCopia);
                e.setRutaCertElectronico(rutaNuevaCopia+"\\Archivos Permanentes\\Cert. Electrónico");
                e.setRutaLog(rutaNuevaCopia+"\\Log");
            }
        }

        // --- 2. ASIGNACIÓN DE DATOS ---

        e.setNifCif(txtNifCif.getText().trim());
        e.setDenominacionSocial(nombreNuevo);
        e.setFormaSocial(formaNueva);
        e.setActivo(esActivo);
        e.setAbreviatura(txtAbreviatura.getText().trim());
        e.setServicio(comboServicio.getValue());
        e.setDelegacion(comboDelegacion.getValue());
        e.setAgenteContable(comboAgente.getValue());

        e.setContactoNombre(txtCNombre.getText().trim());
        e.setContactoMovil(txtCMovil.getText().trim());
        e.setContactoMail(txtCEmail.getText().trim());
        e.setContactoDNI(txtCNif.getText().trim());

        // --- 3. GUARDADO EN BASE DE DATOS ---
        boolean ok = false;
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
                List<Persona> actualesBD = DatabaseManager.obtenerPersonasDeEmpresa(e.getNifCif());
                List<String> nifsActuales = actualesBD.stream().map(Persona::getNif).toList();
                List<String> nifsNuevos = personasSeleccionadas.stream().map(Persona::getNif).toList();

                // 1. Vincular las nuevas
                for (Persona p : personasSeleccionadas) {
                    if (!nifsActuales.contains(p.getNif())) {
                        empService.vincularPersona(e.getNifCif(), p.getNif());
                    }
                }
                // 2. Desvincular las que el usuario ha borrado
                for (Persona pBD : actualesBD) {
                    if (!nifsNuevos.contains(pBD.getNif())) {
                        empService.desvincularPersona(e.getNifCif(), pBD.getNif());
                    }
                }
            }
        }

        // --- 4. ACCIONES POST-GUARDADO ---
        if (ok) {
            // Si todo fue bien en BD y se requiere mover la carpeta, lo hacemos ahora:
            if (requiereMoverCarpeta && rutaAntigua != null && rutaNueva != null) {
                moverCarpetaFisica(rutaAntigua, rutaNueva);
            }

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

        String denominacionActual = txtDenominacion.getText().trim();
        if (denominacionActual.isBlank())  {
            mostrarReq("Denominación Social");
            return false;
        }

        // --- Denominación Social y Abreviatura Única ---
        List<Empresa> todasLasEmpresas = empService.obtenerTodas();
        if (todasLasEmpresas != null) {
            for (Empresa emp : todasLasEmpresas) {
                // Comparamos ignorando mayúsculas/minúsculas para mayor seguridad
                if (emp.getDenominacionSocial().equalsIgnoreCase(denominacionActual)) {
                    // Si estamos creando (empresaEditar == null) ya es un duplicado.
                    // Si estamos editando, es duplicado SOLO si el NIF es de OTRA empresa distinta.
                    if (empresaEditar == null || !emp.getNifCif().equals(empresaEditar.getNifCif())) {
                        mostrarAlerta("Ya existe otra empresa registrada como '" + denominacionActual + "'.\n" +
                                "Para evitar conflictos de carpetas, la denominación social debe ser única.");
                        return false;
                    }
                }
            }
        }

        String abreviaturaActual = txtAbreviatura.getText().trim();
        if (abreviaturaActual.isBlank())  {
            mostrarReq("Abreviatura");
            return false;
        }

        if (todasLasEmpresas != null) {
            for (Empresa emp : todasLasEmpresas) {
                if (emp.getAbreviatura().equalsIgnoreCase(abreviaturaActual) && !emp.getNifCif().equals(cif)) {
                    mostrarAlerta("Ya existe una empresa con esa misma abreviatura");
                    return false;
                }
            }
        }

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
