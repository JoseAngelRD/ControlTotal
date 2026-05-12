package com.example.controltotal_proyecto.entities;

import javafx.beans.property.*;

/**
 * Entidad Empresa — incluye todos los campos del sistema AuditGest.
 * Usa JavaFX Properties para integración directa con TableView y Bindings.
 *
 * NOTA: La base de datos debe incluir las columnas:
 *   servicio VARCHAR(50), delegacion VARCHAR(80), fecha_alta DATE
 * Ejecuta el script: ALTER TABLE empresas ADD COLUMN servicio VARCHAR(50),
 *   ADD COLUMN delegacion VARCHAR(80), ADD COLUMN fecha_alta DATE;
 */
public class Empresa implements Comparable<Empresa> {

    // ─── JavaFX Properties (para TableView y Bindings) ───────────────────────
    private final StringProperty  nifCif             = new SimpleStringProperty();
    private final StringProperty  denominacionSocial = new SimpleStringProperty();
    private final StringProperty  formaSocial        = new SimpleStringProperty();
    private final BooleanProperty activo             = new SimpleBooleanProperty(true);
    private final StringProperty  abreviatura        = new SimpleStringProperty();
    private final StringProperty  contactoNombre     = new SimpleStringProperty();
    private final StringProperty  contactoMovil      = new SimpleStringProperty();
    private final StringProperty  contactoMail       = new SimpleStringProperty();
    private final StringProperty  contactoDNI       = new SimpleStringProperty();
    private final StringProperty  agenteContable     = new SimpleStringProperty();
    private final StringProperty  rutaDocumental     = new SimpleStringProperty();
    private final StringProperty  rutaCertElectronico= new SimpleStringProperty();
    private final StringProperty  rutaLog            = new SimpleStringProperty();
    private final StringProperty  servicio           = new SimpleStringProperty();
    private final StringProperty  delegacion         = new SimpleStringProperty();
    private final StringProperty  fechaAlta          = new SimpleStringProperty();

    // ─── Constructores ────────────────────────────────────────────────────────

    public Empresa() {}

    /** Constructor completo para uso en servicio / base de datos */
    public Empresa(String nifCif, String denominacionSocial, String formaSocial,
                   boolean activo, String abreviatura, String contactoNombre,
                   String contactoMovil, String contactoMail, String contactoDNI, String agenteContable,
                   String servicio, String delegacion, String fechaAlta,
                   String rutaDocumental, String rutaCertElectronico, String rutaLog) {
        setNifCif(nifCif);
        setDenominacionSocial(denominacionSocial);
        setFormaSocial(formaSocial);
        setActivo(activo);
        setAbreviatura(abreviatura);
        setContactoNombre(contactoNombre);
        setContactoMovil(contactoMovil);
        setContactoMail(contactoMail);
        setContactoDNI(contactoDNI);
        setAgenteContable(agenteContable);
        setServicio(servicio);
        setDelegacion(delegacion);
        setFechaAlta(fechaAlta);
        setRutaDocumental(rutaDocumental);
        setRutaCertElectronico(rutaCertElectronico);
        setRutaLog(rutaLog);
    }

    /** Constructor mínimo para pruebas */
    public Empresa(String nif, String nom, String forma) {
        setNifCif(nif);
        setDenominacionSocial(nom);
        setFormaSocial(forma);
    }

    // ─── Estado legible (para mostrar "Activo" / "Pasivo" en la tabla) ────────

    public String getEstado() {
        return isActivo() ? "Activo" : "Pasivo";
    }

    // ─── Property getters (necesarios para TableView con PropertyValueFactory) ─

    public StringProperty  nifCifProperty()              { return nifCif; }
    public StringProperty  denominacionSocialProperty()  { return denominacionSocial; }
    public StringProperty  formaSocialProperty()         { return formaSocial; }
    public BooleanProperty activoProperty()              { return activo; }
    public StringProperty  abreviaturaProperty()         { return abreviatura; }
    public StringProperty  contactoNombreProperty()      { return contactoNombre; }
    public StringProperty  contactoMovilProperty()       { return contactoMovil; }
    public StringProperty  contactoMailProperty()        { return contactoMail; }
    public StringProperty  contactoDNIProperty()         { return contactoDNI; }
    public StringProperty  agenteContableProperty()      { return agenteContable; }
    public StringProperty  servicioProperty()            { return servicio; }
    public StringProperty  delegacionProperty()          { return delegacion; }
    public StringProperty  fechaAltaProperty()           { return fechaAlta; }
    public StringProperty  rutaDocumentalProperty()      { return rutaDocumental; }
    public StringProperty  rutaCertElectronicoProperty() { return rutaCertElectronico; }
    public StringProperty  rutaLogProperty()             { return rutaLog; }

    // ─── Getters / Setters estándar ────────────────────────────────────────────

    public String  getNifCif()              { return nifCif.get(); }
    public void    setNifCif(String v)      { nifCif.set(v); }

    public String  getDenominacionSocial()         { return denominacionSocial.get(); }
    public void    setDenominacionSocial(String v) { denominacionSocial.set(v); }

    public String  getFormaSocial()         { return formaSocial.get(); }
    public void    setFormaSocial(String v) { formaSocial.set(v); }

    public boolean isActivo()               { return activo.get(); }
    public void    setActivo(boolean v)     { activo.set(v); }

    public String  getAbreviatura()         { return abreviatura.get(); }
    public void    setAbreviatura(String v) { abreviatura.set(v); }

    public String  getContactoNombre()         { return contactoNombre.get(); }
    public void    setContactoNombre(String v) { contactoNombre.set(v); }

    public String  getContactoMovil()         { return contactoMovil.get(); }
    public void    setContactoMovil(String v) { contactoMovil.set(v); }

    public String  getContactoMail()         { return contactoMail.get(); }
    public void    setContactoMail(String v) { contactoMail.set(v); }

    public String  getContactoDNI()         { return contactoDNI.get(); }
    public void    setContactoDNI(String v) { contactoDNI.set(v); }

    public String  getAgenteContable()         { return agenteContable.get(); }
    public void    setAgenteContable(String v) { agenteContable.set(v); }

    public String  getServicio()         { return servicio.get(); }
    public void    setServicio(String v) { servicio.set(v); }

    public String  getDelegacion()         { return delegacion.get(); }
    public void    setDelegacion(String v) { delegacion.set(v); }

    public String  getFechaAlta()         { return fechaAlta.get(); }
    public void    setFechaAlta(String v) { fechaAlta.set(v); }

    public String  getRutaDocumental()         { return rutaDocumental.get(); }
    public void    setRutaDocumental(String v) { rutaDocumental.set(v); }

    public String  getRutaCertElectronico()         { return rutaCertElectronico.get(); }
    public void    setRutaCertElectronico(String v) { rutaCertElectronico.set(v); }

    public String  getRutaLog()         { return rutaLog.get(); }
    public void    setRutaLog(String v) { rutaLog.set(v); }

    @Override
    public int compareTo(Empresa otraEmpresa) {
        // Evitar errores si alguna empresa no tiene nombre
        if (this.getDenominacionSocial() == null) return -1;
        if (otraEmpresa.getDenominacionSocial() == null) return 1;

        // Ordena alfabéticamente por Denominación Social
        return this.getDenominacionSocial().compareToIgnoreCase(otraEmpresa.getDenominacionSocial());
    }

    @Override
    public String toString() {
        return getDenominacionSocial() + " (" + getNifCif() + ")";
    }
}
