package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.service.PersonaService;
import com.example.controltotal_proyecto.util.AlertaUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controlador del formulario de creación/edición de Persona.
 *
 * CAMBIOS respecto a la versión anterior:
 *  - Añadido setCloseAction(Runnable) para cierre inline (sin Stage modal).
 *  - cerrar() usa closeAction si está disponible, o Stage.close() como fallback.
 *  - Las alertas usan AlertaUtil en lugar de new Alert, igual que EmpresaFormController.
 *  - Lógica de detección de cambio de ruta y moverCarpetaFisica, igual que en Empresa.
 *  - dialogTitle actualiza el título del formulario según modo creación/edición.
 */
public class PersonaFormController implements Initializable {

    private static final String INICIO_RUTA = "C:/Control_Total/";   // Y:/DocumOfi/

    // ─── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label            dialogTitle;
    @FXML private TextField        txtNif;
    @FXML private TextField        txtNombre;
    @FXML private TextField        txtApellidos;
    @FXML private TextField        txtMovil;
    @FXML private TextField        txtEmail;
    @FXML private ComboBox<String> comboEstado;
    @FXML private Label            lblRuta;

    // ─── Estado interno ───────────────────────────────────────────────────────
    private final PersonaService service = new PersonaService();
    private Persona              personaEditar;
    private Consumer<Persona>    callback;
    private Runnable             closeAction;   // ← NUEVO: acción de cierre inline

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboEstado.getItems().addAll("Activo", "Pasivo");
        comboEstado.setValue("Activo");

        txtNombre.textProperty().addListener((obs, o, n) -> actualizarRuta());
        txtApellidos.textProperty().addListener((obs, o, n) -> actualizarRuta());
        comboEstado.valueProperty().addListener((obs, o, n) -> actualizarRuta());
        actualizarRuta();
    }

    /**
     * Registra el Runnable que vuelve a la vista lista al cancelar/guardar.
     * Equivalente a EmpresaFormController.setCloseAction().
     */
    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    /**
     * Inicializa el formulario con una persona existente (edición) o null (nueva).
     */
    public void init(Persona persona, Consumer<Persona> callback) {
        this.personaEditar = persona;
        this.callback      = callback;

        if (persona != null) {
            txtNif.setText(persona.getNif());
            txtNif.setDisable(true);
            txtNombre.setText(persona.getNombre());
            txtApellidos.setText(persona.getApellidos());
            txtMovil.setText(persona.getContactoMovil());
            txtEmail.setText(persona.getContactoMail());
            comboEstado.setValue(persona.isActivo() ? "Activo" : "Pasivo");
            lblRuta.setText(persona.getRutaDocumental() != null ? persona.getRutaDocumental() : "—");

            dialogTitle.setText("Modificar Persona");
        } else {
            dialogTitle.setText("Nueva Persona");
        }

        actualizarRuta();
    }

    // ─── Previsualización de ruta ─────────────────────────────────────────────

    private String construirRuta(boolean isActivo, String nombre, String apellidos) {
        String estado      = isActivo ? "Activo" : "Pasivo";
        String nombreCompl = nombre.trim();
        if (apellidos != null && !apellidos.isBlank()) {
            nombreCompl += " " + apellidos.trim();
        }
        return INICIO_RUTA + estado + "/Personas/" + nombreCompl;
    }

    private void actualizarRuta() {
        String nombre    = txtNombre.getText().trim();
        String apellidos = txtApellidos.getText().trim();

        if (nombre.isBlank()) {
            lblRuta.setText(INICIO_RUTA + comboEstado.getValue() + "/Personas/");
        } else {
            lblRuta.setText(construirRuta("Activo".equals(comboEstado.getValue()), nombre, apellidos) + "/");
        }
    }

    // ─── Mover carpeta física ─────────────────────────────────────────────────

    private void moverCarpetaFisica(String rutaOrigen, String rutaDestino) {
        Path origen  = Paths.get(rutaOrigen);
        Path destino = Paths.get(rutaDestino);
        if (Files.exists(origen) && !rutaOrigen.equals(rutaDestino)) {
            try {
                Files.createDirectories(destino.getParent());
                Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
                mostrarAlerta(
                        "Se guardó la persona en base de datos, pero hubo un error al mover la carpeta.\n" +
                                "Por favor, muévela manualmente de:\n" + rutaOrigen + "\na:\n" + rutaDestino
                );
            }
        }
    }

    // ─── Guardar ──────────────────────────────────────────────────────────────

    @FXML private void onGuardar() {
        if (!validar()) return;

        boolean esActivo     = "Activo".equals(comboEstado.getValue());
        String  nombreNuevo  = txtNombre.getText().trim();
        String  apellidosNuevo = txtApellidos.getText().trim();

        // 1. Detectar cambios de ruta ANTES de modificar el objeto
        boolean requiereMoverCarpeta = false;
        String  rutaAntigua = null;
        String  rutaNueva   = null;

        if (personaEditar != null) {
            String  nombreViejo    = personaEditar.getNombre();
            String  apellidosViejo = personaEditar.getApellidos();
            boolean estadoViejo    = personaEditar.isActivo();

            if (estadoViejo != esActivo
                    || !nombreViejo.equals(nombreNuevo)
                    || !apellidosViejo.equals(apellidosNuevo)) {
                rutaAntigua = construirRuta(estadoViejo, nombreViejo, apellidosViejo);
                rutaNueva   = construirRuta(esActivo,    nombreNuevo, apellidosNuevo);
                requiereMoverCarpeta = true;
            }
        }

        // 2. Asignación de datos
        Persona p = personaEditar != null ? personaEditar : new Persona();
        p.setNif(txtNif.getText().trim());
        p.setNombre(nombreNuevo);
        p.setApellidos(apellidosNuevo);
        p.setContactoMovil(txtMovil.getText().trim());
        p.setContactoMail(txtEmail.getText().trim());
        p.setActivo(esActivo);

        // 3. Guardado en base de datos
        boolean ok = personaEditar == null ? service.crear(p) : service.actualizar(p);

        // 4. Acciones post-guardado
        if (ok) {
            if (requiereMoverCarpeta && rutaAntigua != null && rutaNueva != null) {
                moverCarpetaFisica(rutaAntigua, rutaNueva);
            }
            if (callback != null) callback.accept(p);
            cerrar();
        } else {
            mostrarAlerta(personaEditar == null
                    ? "El NIF que has introducido ya existe."
                    : "No se pudo actualizar la persona.");
        }
    }

    @FXML private void onCancelar() { cerrar(); }

    /**
     * Cierra el formulario: usa closeAction (modo inline) o Stage.close() (fallback modal).
     * Equivalente a EmpresaFormController.cerrar().
     */
    private void cerrar() {
        if (closeAction != null) {
            closeAction.run();
        } else {
            ((javafx.stage.Stage) txtNif.getScene().getWindow()).close();
        }
    }

    // ─── Validación ───────────────────────────────────────────────────────────

    private boolean validar() {
        if (txtNif.getText().isBlank())       { mostrarReq("NIF");       return false; }
        if (txtNombre.getText().isBlank())     { mostrarReq("Nombre");    return false; }
        if (txtApellidos.getText().isBlank())  { mostrarReq("Apellidos"); return false; }
        return true;
    }

    private void mostrarReq(String campo) {
        mostrarAlerta("El campo \"" + campo + "\" es obligatorio.");
    }

    private void mostrarAlerta(String mensaje) {
        Window owner = txtNif.getScene() != null
                ? txtNif.getScene().getWindow()
                : null;
        AlertaUtil.mostrarAdvertencia(mensaje, owner);
    }
}
