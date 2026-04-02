package com.example.controltotal_proyecto.bd;

import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de base de datos — CRUD completo para Empresa y Persona.
 *
 * ESQUEMA REQUERIDO (ejecutar en MySQL si no existe):
 * ─────────────────────────────────────────────────────────────────────────────
 * CREATE TABLE IF NOT EXISTS empresas (
 *   nif_cif               VARCHAR(20)  PRIMARY KEY,
 *   denominacion_social   VARCHAR(200) NOT NULL,
 *   forma_social          VARCHAR(50),
 *   activo                TINYINT(1)   DEFAULT 1,
 *   abreviatura           VARCHAR(20),
 *   contacto_nombre       VARCHAR(100),
 *   contacto_movil        VARCHAR(20),
 *   contacto_mail         VARCHAR(100),
 *   agente_contable       VARCHAR(100),
 *   servicio              VARCHAR(50),
 *   delegacion            VARCHAR(80),
 *   fecha_alta            VARCHAR(20),
 *   ruta_documental       VARCHAR(500),
 *   ruta_cert_electronico VARCHAR(500),
 *   ruta_log              VARCHAR(500)
 * );
 *
 * CREATE TABLE IF NOT EXISTS personas (
 *   nif                   VARCHAR(20)  PRIMARY KEY,
 *   apellidos             VARCHAR(100) NOT NULL,
 *   nombre                VARCHAR(100) NOT NULL,
 *   activo                TINYINT(1)   DEFAULT 1,
 *   contacto_movil        VARCHAR(20),
 *   contacto_mail         VARCHAR(100),
 *   ruta_documental       VARCHAR(500),
 *   ruta_cert_electronico VARCHAR(500),
 *   ruta_log              VARCHAR(500)
 * );
 *
 * CREATE TABLE IF NOT EXISTS empresa_persona (
 *   empresa_nif  VARCHAR(20) NOT NULL,
 *   persona_nif  VARCHAR(20) NOT NULL,
 *   PRIMARY KEY (empresa_nif, persona_nif),
 *   FOREIGN KEY (empresa_nif) REFERENCES empresas(nif_cif) ON DELETE CASCADE,
 *   FOREIGN KEY (persona_nif) REFERENCES personas(nif)     ON DELETE CASCADE
 * );
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DatabaseManager {

    // ══════════════════════════════════════════════════════════════════════════
    // EMPRESA — CRUD
    // ══════════════════════════════════════════════════════════════════════════

    /** Inserta una nueva empresa. Devuelve true si tuvo éxito. */
    public static boolean guardarEmpresa(Empresa e) {
        String sql = "INSERT INTO empresas VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setEmpresaParams(ps, e);
            ps.executeUpdate();
            System.out.println("✅ Empresa guardada: " + e.getDenominacionSocial());
            return true;
        } catch (SQLException ex) {
            System.err.println("❌ Error al guardar empresa: " + ex.getMessage());
            return false;
        }
    }

    /** Actualiza una empresa existente. */
    public static boolean actualizarEmpresa(Empresa e) {
        String sql = """
            UPDATE empresas SET
              denominacion_social=?, forma_social=?, activo=?, abreviatura=?,
              contacto_nombre=?, contacto_movil=?, contacto_mail=?,
              agente_contable=?, servicio=?, delegacion=?, fecha_alta=?,
              ruta_documental=?, ruta_cert_electronico=?, ruta_log=?
            WHERE nif_cif=?
            """;
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  e.getDenominacionSocial());
            ps.setString(2,  e.getFormaSocial());
            ps.setBoolean(3, e.isActivo());
            ps.setString(4,  e.getAbreviatura());
            ps.setString(5,  e.getContactoNombre());
            ps.setString(6,  e.getContactoMovil());
            ps.setString(7,  e.getContactoMail());
            ps.setString(8,  e.getAgenteContable());
            ps.setString(9,  e.getServicio());
            ps.setString(10, e.getDelegacion());
            ps.setString(11, e.getFechaAlta());
            ps.setString(12, e.getRutaDocumental());
            ps.setString(13, e.getRutaCertElectronico());
            ps.setString(14, e.getRutaLog());
            ps.setString(15, e.getNifCif());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Error al actualizar empresa: " + ex.getMessage());
            return false;
        }
    }

    /** Elimina una empresa por NIF/CIF. */
    public static boolean eliminarEmpresa(String nifCif) {
        String sql = "DELETE FROM empresas WHERE nif_cif=?";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nifCif);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Error al eliminar empresa: " + ex.getMessage());
            return false;
        }
    }

    /** Obtiene todas las empresas. */
    public static List<Empresa> obtenerTodasLasEmpresas() {
        List<Empresa> lista = new ArrayList<>();
        String sql = "SELECT * FROM empresas ORDER BY denominacion_social";
        try (Connection c = ConexionBD.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapEmpresa(rs));
        } catch (SQLException ex) {
            System.err.println("❌ Error al obtener empresas: " + ex.getMessage());
        }
        return lista;
    }

    /** Busca una empresa por NIF/CIF. */
    public static Empresa obtenerEmpresaPorNif(String nifCif) {
        String sql = "SELECT * FROM empresas WHERE nif_cif=?";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nifCif);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapEmpresa(rs);
        } catch (SQLException ex) {
            System.err.println("❌ Error al obtener empresa: " + ex.getMessage());
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PERSONA — CRUD
    // ══════════════════════════════════════════════════════════════════════════

    /** Inserta una nueva persona. */
    public static boolean guardarPersona(Persona p) {
        String sql = "INSERT INTO personas VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setPersonaParams(ps, p);
            ps.executeUpdate();
            System.out.println("✅ Persona guardada: " + p.getNombreCompleto());
            return true;
        } catch (SQLException ex) {
            System.err.println("❌ Error al guardar persona: " + ex.getMessage());
            return false;
        }
    }

    /** Actualiza una persona existente. */
    public static boolean actualizarPersona(Persona p) {
        String sql = """
            UPDATE personas SET
              apellidos=?, nombre=?, activo=?,
              contacto_movil=?, contacto_mail=?,
              ruta_documental=?, ruta_cert_electronico=?, ruta_log=?
            WHERE nif=?
            """;
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  p.getApellidos());
            ps.setString(2,  p.getNombre());
            ps.setBoolean(3, p.isActivo());
            ps.setString(4,  p.getContactoMovil());
            ps.setString(5,  p.getContactoMail());
            ps.setString(6,  p.getRutaDocumental());
            ps.setString(7,  p.getRutaCertElectronico());
            ps.setString(8,  p.getRutaLog());
            ps.setString(9,  p.getNif());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Error al actualizar persona: " + ex.getMessage());
            return false;
        }
    }

    /** Elimina una persona por NIF. */
    public static boolean eliminarPersona(String nif) {
        String sql = "DELETE FROM personas WHERE nif=?";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nif);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Error al eliminar persona: " + ex.getMessage());
            return false;
        }
    }

    /** Obtiene todas las personas. */
    public static List<Persona> obtenerTodasLasPersonas() {
        List<Persona> lista = new ArrayList<>();
        String sql = "SELECT * FROM personas ORDER BY apellidos, nombre";
        try (Connection c = ConexionBD.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapPersona(rs));
        } catch (SQLException ex) {
            System.err.println("❌ Error al obtener personas: " + ex.getMessage());
        }
        return lista;
    }

    /** Busca una persona por NIF. */
    public static Persona obtenerPersonaPorNif(String nif) {
        String sql = "SELECT * FROM personas WHERE nif=?";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nif);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapPersona(rs);
        } catch (SQLException ex) {
            System.err.println("❌ Error al obtener persona: " + ex.getMessage());
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RELACIONES empresa_persona
    // ══════════════════════════════════════════════════════════════════════════

    /** Vincula una empresa con una persona. */
    public static boolean vincularEmpresaPersona(String empresaNif, String personaNif) {
        String sql = "INSERT IGNORE INTO empresa_persona (empresa_nif, persona_nif) VALUES (?,?)";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, empresaNif);
            ps.setString(2, personaNif);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Error al vincular empresa-persona: " + ex.getMessage());
            return false;
        }
    }

    /** Desvincula una empresa de una persona. */
    public static boolean desvincularEmpresaPersona(String empresaNif, String personaNif) {
        String sql = "DELETE FROM empresa_persona WHERE empresa_nif=? AND persona_nif=?";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, empresaNif);
            ps.setString(2, personaNif);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Error al desvincular: " + ex.getMessage());
            return false;
        }
    }

    /** Devuelve los NIF de personas vinculadas a una empresa. */
    public static List<String> obtenerPersonasDeEmpresa(String empresaNif) {
        List<String> nifs = new ArrayList<>();
        String sql = "SELECT persona_nif FROM empresa_persona WHERE empresa_nif=?";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, empresaNif);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) nifs.add(rs.getString("persona_nif"));
        } catch (SQLException ex) {
            System.err.println("❌ Error: " + ex.getMessage());
        }
        return nifs;
    }

    /** Devuelve los NIF de empresas vinculadas a una persona. */
    public static List<String> obtenerEmpresasDePersona(String personaNif) {
        List<String> nifs = new ArrayList<>();
        String sql = "SELECT empresa_nif FROM empresa_persona WHERE persona_nif=?";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, personaNif);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) nifs.add(rs.getString("empresa_nif"));
        } catch (SQLException ex) {
            System.err.println("❌ Error: " + ex.getMessage());
        }
        return nifs;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS — mappers y setters de parámetros
    // ══════════════════════════════════════════════════════════════════════════

    private static void setEmpresaParams(PreparedStatement ps, Empresa e) throws SQLException {
        ps.setString(1,  e.getNifCif());
        ps.setString(2,  e.getDenominacionSocial());
        ps.setString(3,  e.getFormaSocial());
        ps.setBoolean(4, e.isActivo());
        ps.setString(5,  e.getAbreviatura());
        ps.setString(6,  e.getContactoNombre());
        ps.setString(7,  e.getContactoMovil());
        ps.setString(8,  e.getContactoMail());
        ps.setString(9,  e.getAgenteContable());
        ps.setString(10, e.getRutaDocumental());
        ps.setString(11, e.getRutaCertElectronico());
        ps.setString(12, e.getRutaLog());
        ps.setString(13, e.getServicio());
        ps.setString(14, e.getDelegacion());
        ps.setString(15, e.getFechaAlta());
    }

    private static void setPersonaParams(PreparedStatement ps, Persona p) throws SQLException {
        ps.setString(1,  p.getNif());
        ps.setString(2,  p.getApellidos());
        ps.setString(3,  p.getNombre());
        ps.setBoolean(4, p.isActivo());
        ps.setString(5,  p.getContactoMovil());
        ps.setString(6,  p.getContactoMail());
        ps.setString(7,  p.getRutaDocumental());
        ps.setString(8,  p.getRutaCertElectronico());
        ps.setString(9,  p.getRutaLog());
    }

    private static Empresa mapEmpresa(ResultSet rs) throws SQLException {
        Empresa e = new Empresa();
        e.setNifCif(rs.getString("nif_cif"));
        e.setDenominacionSocial(rs.getString("denominacion_social"));
        e.setFormaSocial(rs.getString("forma_social"));
        e.setActivo(rs.getBoolean("activo"));
        e.setAbreviatura(rs.getString("abreviatura"));
        e.setContactoNombre(rs.getString("contacto_nombre"));
        e.setContactoMovil(rs.getString("contacto_movil"));
        e.setContactoMail(rs.getString("contacto_mail"));
        e.setAgenteContable(rs.getString("agente_contable"));
        e.setServicio(rs.getString("servicio"));
        e.setDelegacion(rs.getString("delegacion"));
        e.setFechaAlta(rs.getString("fecha_alta"));
        e.setRutaDocumental(rs.getString("ruta_documental"));
        e.setRutaCertElectronico(rs.getString("ruta_cert_electronico"));
        e.setRutaLog(rs.getString("ruta_log"));
        return e;
    }

    private static Persona mapPersona(ResultSet rs) throws SQLException {
        Persona p = new Persona();
        p.setNif(rs.getString("nif"));
        p.setApellidos(rs.getString("apellidos"));
        p.setNombre(rs.getString("nombre"));
        p.setActivo(rs.getBoolean("activo"));
        p.setContactoMovil(rs.getString("contacto_movil"));
        p.setContactoMail(rs.getString("contacto_mail"));
        p.setRutaDocumental(rs.getString("ruta_documental"));
        p.setRutaCertElectronico(rs.getString("ruta_cert_electronico"));
        p.setRutaLog(rs.getString("ruta_log"));
        return p;
    }
}
