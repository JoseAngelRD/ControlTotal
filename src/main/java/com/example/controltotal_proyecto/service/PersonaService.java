package com.example.controltotal_proyecto.service;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.util.Directorios;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Capa de servicio para Persona.
 */
public class PersonaService {

    // ─── Consultas ────────────────────────────────────────────────────────────

    public List<Persona> obtenerTodas() {
        return DatabaseManager.obtenerTodasLasPersonas();
    }

    public Persona obtenerPorNif(String nif) {
        return DatabaseManager.obtenerPersonaPorNif(nif);
    }

    /**
     * Devuelve las empresas a las que está vinculada esta persona.
     */
    public List<Empresa> obtenerEmpresasDePersona(String personaNif) {
        List<String> empNifs = DatabaseManager.obtenerEmpresasDePersona(personaNif);
        return empNifs.stream()
            .map(DatabaseManager::obtenerEmpresaPorNif)
            .filter(e -> e != null)
            .collect(Collectors.toList());
    }

    // ─── Alta ─────────────────────────────────────────────────────────────────

    public boolean crear(Persona persona) {
        // Generar ruta documental automáticamente
        String rutaBase = Directorios.crearCarpetaPersona(persona.getNombre(), persona.isActivo());
        if (rutaBase != null) {
            persona.setRutaDocumental(rutaBase);
            persona.setRutaCertElectronico(rutaBase + "\\Certificado Electrónico");
            persona.setRutaLog(rutaBase + "\\Log");
        }

        //Guardar Empresa BD
        return DatabaseManager.guardarPersona(persona);
    }

    // ─── Edición ──────────────────────────────────────────────────────────────

    public boolean actualizar(Persona persona) {
        return DatabaseManager.actualizarPersona(persona);
    }

    // ─── Baja ─────────────────────────────────────────────────────────────────

    public boolean eliminar(String nif) {
        return DatabaseManager.eliminarPersona(nif);
    }

    // ─── Estado automático ────────────────────────────────────────────────────

    /**
     * Recalcula el estado de una persona: si todas sus empresas vinculadas
     * están en pasivo, la persona pasa a Inactivo.
     */
    public void recalcularEstado(String personaNif) {
        List<Empresa> empresas = obtenerEmpresasDePersona(personaNif);
        if (empresas.isEmpty()) return;

        boolean hayActiva = empresas.stream().anyMatch(Empresa::isActivo);
        Persona p = obtenerPorNif(personaNif);
        if (p != null) {
            p.setActivo(hayActiva);
            actualizar(p);
        }
    }
}
