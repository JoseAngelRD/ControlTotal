package com.example.controltotal_proyecto.entities;

import javafx.beans.property.*;

/**
 * Entidad Persona con JavaFX Properties para integración con TableView.
 */
public class Persona {

    private final StringProperty  nif                 = new SimpleStringProperty();
    private final StringProperty  apellidos           = new SimpleStringProperty();
    private final StringProperty  nombre              = new SimpleStringProperty();
    private final BooleanProperty activo              = new SimpleBooleanProperty(true);
    private final StringProperty  contactoMovil       = new SimpleStringProperty();
    private final StringProperty  contactoMail        = new SimpleStringProperty();
    private final StringProperty  rutaDocumental      = new SimpleStringProperty();
    private final StringProperty  rutaCertElectronico = new SimpleStringProperty();
    private final StringProperty  rutaLog             = new SimpleStringProperty();

    // ─── Constructores ────────────────────────────────────────────────────────

    public Persona() {}

    public Persona(String nif, String apellidos, String nombre, boolean activo,
                   String contactoMovil, String contactoMail,
                   String rutaDocumental, String rutaCertElectronico, String rutaLog) {
        setNif(nif);
        setApellidos(apellidos);
        setNombre(nombre);
        setActivo(activo);
        setContactoMovil(contactoMovil);
        setContactoMail(contactoMail);
        setRutaDocumental(rutaDocumental);
        setRutaCertElectronico(rutaCertElectronico);
        setRutaLog(rutaLog);
    }

    // ─── Nombre completo ──────────────────────────────────────────────────────

    public String getNombreCompleto() {
        return getNombre() + " " + getApellidos();
    }

    public String getEstado() {
        return isActivo() ? "Activo" : "Inactivo";
    }

    // ─── Property getters ─────────────────────────────────────────────────────

    public StringProperty  nifProperty()                 { return nif; }
    public StringProperty  apellidosProperty()           { return apellidos; }
    public StringProperty  nombreProperty()              { return nombre; }
    public BooleanProperty activoProperty()              { return activo; }
    public StringProperty  contactoMovilProperty()       { return contactoMovil; }
    public StringProperty  contactoMailProperty()        { return contactoMail; }
    public StringProperty  rutaDocumentalProperty()      { return rutaDocumental; }
    public StringProperty  rutaCertElectronicoProperty() { return rutaCertElectronico; }
    public StringProperty  rutaLogProperty()             { return rutaLog; }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String  getNif()               { return nif.get(); }
    public void    setNif(String v)       { nif.set(v); }

    public String  getApellidos()         { return apellidos.get(); }
    public void    setApellidos(String v) { apellidos.set(v); }

    public String  getNombre()            { return nombre.get(); }
    public void    setNombre(String v)    { nombre.set(v); }

    public boolean isActivo()             { return activo.get(); }
    public void    setActivo(boolean v)   { activo.set(v); }

    public String  getContactoMovil()         { return contactoMovil.get() != null ? contactoMovil.get() : ""; }
    public void    setContactoMovil(String v) { contactoMovil.set(v); }

    public String  getContactoMail()         { return contactoMail.get() != null ? contactoMail.get() : ""; }
    public void    setContactoMail(String v) { contactoMail.set(v); }

    public String  getRutaDocumental()         { return rutaDocumental.get(); }
    public void    setRutaDocumental(String v) { rutaDocumental.set(v); }

    public String  getRutaCertElectronico()         { return rutaCertElectronico.get(); }
    public void    setRutaCertElectronico(String v) { rutaCertElectronico.set(v); }

    public String  getRutaLog()         { return rutaLog.get(); }
    public void    setRutaLog(String v) { rutaLog.set(v); }

    @Override
    public String toString() {
        return getNombreCompleto() + " (" + getNif() + ")";
    }
}
