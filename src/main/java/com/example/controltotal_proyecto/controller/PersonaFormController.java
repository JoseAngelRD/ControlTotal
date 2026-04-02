package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.service.PersonaService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controlador del diálogo de creación/edición de Persona.
 */
public class PersonaFormController implements Initializable {

    @FXML private TextField        txtNif;
    @FXML private TextField        txtNombre;
    @FXML private TextField        txtApellidos;
    @FXML private TextField        txtMovil;
    @FXML private TextField        txtEmail;
    @FXML private ComboBox<String> comboEstado;
    @FXML private Label            lblRuta;

    private final PersonaService  service = new PersonaService();
    private Persona               personaEditar;
    private Consumer<Persona>     callback;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboEstado.getItems().addAll("Activo", "Inactivo");
        comboEstado.setValue("Activo");
        txtNif.textProperty().addListener((obs, o, n) ->
            lblRuta.setText(n.isBlank() ? "—" : "servidor/personas/" + n.trim() + "/")
        );
    }

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
            comboEstado.setValue(persona.isActivo() ? "Activo" : "Inactivo");
            lblRuta.setText(persona.getRutaDocumental() != null ? persona.getRutaDocumental() : "—");
        }
    }

    @FXML private void onGuardar() {
        if (!validar()) return;

        Persona p = personaEditar != null ? personaEditar : new Persona();
        p.setNif(txtNif.getText().trim());
        p.setNombre(txtNombre.getText().trim());
        p.setApellidos(txtApellidos.getText().trim());
        p.setContactoMovil(txtMovil.getText().trim());
        p.setContactoMail(txtEmail.getText().trim());
        p.setActivo("Activo".equals(comboEstado.getValue()));

        boolean ok = personaEditar == null ? service.crear(p) : service.actualizar(p);

        if (ok) {
            if (callback != null) callback.accept(p);
            cerrar();
        } else {
            new Alert(Alert.AlertType.ERROR,
                personaEditar == null
                    ? "No se pudo crear la persona. ¿Existe ya ese NIF?"
                    : "No se pudo actualizar la persona."
            ).show();
        }
    }

    @FXML private void onCancelar() { cerrar(); }

    private boolean validar() {
        if (txtNif.getText().isBlank())       { mostrarReq("NIF");      return false; }
        if (txtNombre.getText().isBlank())     { mostrarReq("Nombre");   return false; }
        if (txtApellidos.getText().isBlank())  { mostrarReq("Apellidos");return false; }
        return true;
    }

    private void mostrarReq(String campo) {
        new Alert(Alert.AlertType.WARNING, "El campo \"" + campo + "\" es obligatorio.").show();
    }

    private void cerrar() {
        ((Stage) txtNif.getScene().getWindow()).close();
    }
}
