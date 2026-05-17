package com.example.controltotal_proyecto.service;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.util.Directorios;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Capa de servicio para Persona.
 */
public class PersonaService {

    // --- Inicio de la ruta para construir su recíproco ------------------------
    private static final String INICIO_RUTA = "C:/Control_Total/";   // Y:/DocumOfi/

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
        return DatabaseManager.obtenerEmpresasDePersona(personaNif);
    }

    // ─── Alta ─────────────────────────────────────────────────────────────────

    public boolean crear(Persona persona) {
        // Generar ruta documental automáticamente

        String nombreApellidos = persona.getNombre();
        nombreApellidos += " " + persona.getApellidos();

        String rutaBase = Directorios.crearCarpetaPersona(nombreApellidos, persona.isActivo());
        if (rutaBase != null) {
            persona.setRutaDocumental(rutaBase);
            persona.setRutaCertElectronico(rutaBase + "\\Certificado Electrónico");
            persona.setRutaLog(rutaBase + "\\Log");
        }

        // Guardar Persona BD
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
    public void recalcularEstado(String personaNif) throws IOException {
        Persona p = obtenerPorNif(personaNif);
        List<Empresa> empresas = obtenerEmpresasDePersona(personaNif);
        boolean hayActiva = empresas.stream().anyMatch(Empresa::isActivo);
        boolean nuevoEstado = !empresas.isEmpty() && hayActiva;

        // Mueve la carpeta
        // Si la persona se queda sin empresa (fallos posibles para actualizar la ruta) y la persona es activa
        // O si sus empresas pasan todas a pasivo y la persona es activa
        // O si se le asigna empresa activa y la persona es pasiva
        // cambian las carpetas y su estado
        moverCarpetaFisica(personaNif, nuevoEstado);

        // Se actualiza el estado de la persona en dos casos
        if (empresas.isEmpty()) {
            p.setActivo(false);
            actualizar(p);
            return;
        }

        if (p != null) {
            p.setActivo(hayActiva);
            actualizar(p);
        }
    }

    // Sobrecargado para que funcione calculando la ruta recíproca
    public void moverCarpetaFisica(String rutaOrigen, String rutaDestino) throws IOException {
        Path origen  = Paths.get(rutaOrigen);
        Path destino = Paths.get(rutaDestino);
        if (Files.exists(origen) && !rutaOrigen.equals(rutaDestino)) {
            Files.createDirectories(destino.getParent());
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // Se introduce el nif y el nuevo estado de la persona
    public void moverCarpetaFisica(String personaNif, boolean nuevoEstado) throws IOException {
        Persona p = obtenerPorNif(personaNif);
        boolean estadoViejo = p.isActivo();
        String estado;

        estado = (nuevoEstado ? "Activo" : "Pasivo");

        String nombreCompl = p.getNombreCompleto();

        if (estadoViejo == nuevoEstado)
            System.out.println("ALGO HA SALIDO VERDADERAMENTE MAL");

        // Ruta antigua
        String rutaAntigua = INICIO_RUTA + (estadoViejo ? "Activo" : "Pasivo") + "/Personas/" + nombreCompl;

        // Calcular ruta nueva
        String rutaNueva = INICIO_RUTA + estado + "/Personas/" + nombreCompl;

        String rutaNuevaCopia = rutaNueva;

        // Cambiar rutas documental, certificado electrónico y log
        rutaNuevaCopia = rutaNuevaCopia.replace("/", "\\");
        p.setRutaDocumental(rutaNuevaCopia);
        p.setRutaCertElectronico(rutaNuevaCopia+"\\Certificado Electrónico");
        p.setRutaLog(rutaNuevaCopia+"\\Log");

        moverCarpetaFisica(rutaAntigua, rutaNueva);
    }

}
