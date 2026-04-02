package com.example.controltotal_proyecto.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Gestiona la creación de la estructura de directorios en disco
 * para cada empresa o persona registrada en el sistema.
 *
 * Modifica RUTA_RAIZ para adaptarla a tu entorno de producción.
 */
public class Directorios {

    /** Ruta raíz configurable. Cámbiala según el servidor donde se despliega. */
    private static final String RUTA_RAIZ =
        "C:\\Users\\JoseAngelRD\\Documents\\Programacion\\ControlTotal_JARD\\RutaDirectoriosCT";

    // ─── Subdirectorios estándar para EMPRESA ────────────────────────────────
    private static final List<String> SUBDIRS_EMPRESA = List.of(
        "Archivos Permanentes",
        "Auditoría",
        "Concursal",
        "Contable\\Balances",
        "Contable\\Bancos",
        "Contable\\Facturas",
        "Contable\\Pérdidas y Ganancias",
        "Contable\\Subvenciones",
        "Fiscal",
        "Jurídico",
        "Laboral",
        "Mercantil",
        "Pericial",
        "Persona_relacionada\\Certificado electrónico",
        "Persona_relacionada\\Fiscal",
        "Log"
    );

    // ─── Subdirectorios estándar para PERSONA ────────────────────────────────
    private static final List<String> SUBDIRS_PERSONA = List.of(
        "Certificado electrónico",
        "Documentación",
        "Log"
    );

    // ══════════════════════════════════════════════════════════════════════════
    // API PÚBLICA
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Crea la carpeta de empresa y toda su estructura de subdirectorios.
     *
     * @param nombreCarpeta  Abreviatura de la empresa (nombre de la carpeta raíz).
     * @param esActivo       Si es true va a "activos", si no a "pasivos".
     * @return Ruta absoluta de la carpeta raíz creada, o null si hubo error.
     */
    public static String crearCarpetaEmpresa(String nombreCarpeta, boolean esActivo) {
        String subCarpeta = esActivo ? "activos" : "pasivos";
        Path rutaBase = Paths.get(RUTA_RAIZ, subCarpeta, nombreCarpeta);
        return crearEstructura(rutaBase, SUBDIRS_EMPRESA);
    }

    /**
     * Crea la carpeta de persona y su estructura básica de subdirectorios.
     *
     * @param nif  NIF de la persona (nombre de la carpeta raíz).
     * @return Ruta absoluta de la carpeta raíz creada, o null si hubo error.
     */
    public static String crearCarpetaPersona(String nif) {
        Path rutaBase = Paths.get(RUTA_RAIZ, "personas", nif);
        return crearEstructura(rutaBase, SUBDIRS_PERSONA);
    }

    /**
     * Mueve la carpeta de una empresa de "activos" a "pasivos" o viceversa.
     *
     * @param nombreCarpeta  Abreviatura de la empresa.
     * @param aActivos       true = mover a activos, false = mover a pasivos.
     * @return true si el movimiento tuvo éxito.
     */
    public static boolean moverEmpresa(String nombreCarpeta, boolean aActivos) {
        String origen  = aActivos ? "pasivos" : "activos";
        String destino = aActivos ? "activos"  : "pasivos";

        Path rutaOrigen  = Paths.get(RUTA_RAIZ, origen, nombreCarpeta);
        Path rutaDestino = Paths.get(RUTA_RAIZ, destino, nombreCarpeta);

        if (!Files.exists(rutaOrigen)) {
            System.out.println("⚠️ La carpeta origen no existe: " + rutaOrigen);
            return false;
        }
        try {
            Files.createDirectories(rutaDestino.getParent());
            Files.move(rutaOrigen, rutaDestino);
            System.out.println("📂 Empresa movida a: " + rutaDestino);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Error al mover empresa: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════════════

    private static String crearEstructura(Path rutaBase, List<String> subdirs) {
        try {
            // Crear carpeta raíz
            Files.createDirectories(rutaBase);
            System.out.println("📁 Carpeta raíz creada: " + rutaBase);

            // Crear subdirectorios
            for (String sub : subdirs) {
                Path subPath = rutaBase.resolve(sub);
                if (!Files.exists(subPath)) {
                    Files.createDirectories(subPath);
                }
            }
            return rutaBase.toString();

        } catch (IOException e) {
            System.err.println("❌ Error al crear directorios en " + rutaBase + ": " + e.getMessage());
            return null;
        }
    }
}
