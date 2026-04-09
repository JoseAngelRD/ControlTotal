package com.example.controltotal_proyecto.service;

import com.example.controltotal_proyecto.util.Directorios;
import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Capa de servicio para Empresa.
 * Coordina la lógica de negocio (generación de abreviatura, creación de
 * directorios, vinculación con personas) antes de persistir en la BD.
 */
public class EmpresaService {

    private static final List<String> FORMAS_SKIP =
        List.of("SL","SLL","SA","SAL","CB","SDAD.","COOP.","AND.");

    private static final List<String> AGENTES = List.of(
        "Pedro Sánchez", "María González", "Luis Martín", "Ana Fernández"
    );

    private static final List<String> SERVICIOS   = List.of("Asesoría","Auditoría","Concursal","Pericial");
    private static final List<String> DELEGACIONES = List.of("Huelva","Lepe","Puebla de Guzmán");

    // ─── Consultas ────────────────────────────────────────────────────────────

    public List<Empresa> obtenerTodas() {
        return DatabaseManager.obtenerTodasLasEmpresas();
    }

    public Empresa obtenerPorNif(String nif) {
        return DatabaseManager.obtenerEmpresaPorNif(nif);
    }

    public List<String> getAgentes()     { return AGENTES; }
    public List<String> getServicios()   { return SERVICIOS; }
    public List<String> getDelegaciones(){ return DELEGACIONES; }

    // ─── Alta ─────────────────────────────────────────────────────────────────

    /**
     * Crea una nueva empresa:
     *  1. Genera abreviatura única.
     *  2. Crea estructura de directorios en disco.
     *  3. Persiste en BD.
     *  4. Vincula la persona de contacto si se proporcionó.
     *
     * @return true si todo fue correcto.
     */
    public boolean crear(Empresa empresa, Persona primerContacto) {
        // 1. Generar abreviatura única si está vacía
        if (empresa.getAbreviatura() == null || empresa.getAbreviatura().isBlank()) {
            empresa.setAbreviatura(generarAbreviatura(empresa.getDenominacionSocial()));
        }

        // 2. Fecha de alta
        empresa.setFechaAlta(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // 3. Crear directorios en disco
        String rutaBase = Directorios.crearCarpetaEmpresa(empresa.getDenominacionSocial(),empresa.getFormaSocial(),empresa.isActivo());
        if (rutaBase != null) {
            empresa.setRutaDocumental(rutaBase);
            empresa.setRutaCertElectronico(rutaBase + "\\Archivos Permanentes\\Cert. Electrónico");
            empresa.setRutaLog(rutaBase + "\\Log");
        }

        // 4. Guardar empresa BD
        boolean ok = DatabaseManager.guardarEmpresa(empresa);
        if (!ok) return false;

        // 5. Crear y vincular primer contacto si se proporcionó
        if (primerContacto != null && primerContacto.getNif() != null && !primerContacto.getNif().isBlank()) {
            PersonaService ps = new PersonaService();
            Persona existente = DatabaseManager.obtenerPersonaPorNif(primerContacto.getNif());
            if (existente == null) {
                ps.crear(primerContacto);
            }
            DatabaseManager.vincularEmpresaPersona(empresa.getNifCif(), primerContacto.getNif());
        }

        return true;
    }

    // ─── Edición ──────────────────────────────────────────────────────────────

    public boolean actualizar(Empresa empresa) {
        return DatabaseManager.actualizarEmpresa(empresa);
    }

    // ─── Baja ─────────────────────────────────────────────────────────────────

    public boolean eliminar(String nifCif) {
        return DatabaseManager.eliminarEmpresa(nifCif);
    }

    // ─── Relaciones ───────────────────────────────────────────────────────────

    public boolean vincularPersona(String empresaNif, String personaNif) {
        return DatabaseManager.vincularEmpresaPersona(empresaNif, personaNif);
    }

    public boolean desvincularPersona(String empresaNif, String personaNif) {
        return DatabaseManager.desvincularEmpresaPersona(empresaNif, personaNif);
    }

    public List<String> getPersonasNif(String empresaNif) {
        return DatabaseManager.obtenerPersonasDeEmpresa(empresaNif);
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    /**
     * Genera una abreviatura única a partir de las iniciales de la denominación,
     * excluyendo las formas sociales (SL, SA, etc.).
     */
    public String generarAbreviatura(String denominacion) {
        if (denominacion == null || denominacion.isBlank()) return "EMP";

        String[] words = denominacion.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String w : words) {
            if (!FORMAS_SKIP.contains(w.toUpperCase())) {
                initials.append(Character.toUpperCase(w.charAt(0)));
            }
        }

        String base = initials.isEmpty()
            ? denominacion.substring(0, Math.min(2, denominacion.length())).toUpperCase()
            : initials.toString();

        // Comprobar unicidad contra la BD
        List<Empresa> existentes = DatabaseManager.obtenerTodasLasEmpresas();
        List<String> usadas = existentes.stream().map(Empresa::getAbreviatura).toList();

        if (!usadas.contains(base)) return base;
        int n = 2;
        while (usadas.contains(base + n)) n++;
        return base + n;
    }
}
