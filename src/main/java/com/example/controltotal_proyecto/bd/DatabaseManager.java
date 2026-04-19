package com.example.controltotal_proyecto.bd;

import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    // ══════════════════════════════════════════════════════════════════════════
    // EMPRESA — CRUD
    // ══════════════════════════════════════════════════════════════════════════

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

    public static boolean actualizarEmpresa(Empresa e) {
        // La sentencia UPDATE se adapta al nuevo orden para mantener coherencia
        String sql = """
            UPDATE empresas SET
              denominacion_social=?, forma_social=?, activo=?, abreviatura=?,
              contacto_nombre=?, contacto_movil=?, contacto_mail=?, agente_contable=?,
              ruta_documental=?, ruta_cert_electronico=?, ruta_log=?,
              servicio=?, delegacion=?, fecha_alta=?
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
            ps.setString(9,  e.getRutaDocumental());
            ps.setString(10, e.getRutaCertElectronico());
            ps.setString(11, e.getRutaLog());
            ps.setString(12, e.getServicio());
            ps.setString(13, e.getDelegacion());
            ps.setString(14, e.getFechaAlta());
            ps.setString(15, e.getNifCif());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Error al actualizar empresa: " + ex.getMessage());
            return false;
        }
    }

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

    /** Devuelve los objetos Persona vinculados a una empresa. */
    public static List<Persona> obtenerPersonasDeEmpresa(String empresaNif) {
        List<Persona> personas = new ArrayList<>();
        String sql = """
            SELECT p.* FROM personas p
            INNER JOIN empresa_persona ep ON p.nif = ep.persona_nif
            WHERE ep.empresa_nif = ?
            """;
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, empresaNif);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                personas.add(mapPersona(rs)); // Mapeamos el objeto completo
            }
        } catch (SQLException ex) {
            System.err.println("❌ Error al obtener personas de la empresa: " + ex.getMessage());
        }
        return personas;
    }

    public static List<Empresa> obtenerEmpresasDePersona(String personaNif) {
        List<Empresa> empresas = new ArrayList<>();
        String sql = "SELECT e.* FROM empresas e INNER JOIN empresa_persona ep ON e.nif_cif = ep.empresa_nif WHERE ep.persona_nif = ?";
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, personaNif);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) empresas.add(mapEmpresa(rs));
        } catch (SQLException ex) {
            System.err.println("❌ Error: " + ex.getMessage());
        }
        return empresas;
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
        ps.setString(10, e.getRutaDocumental());         // <-- Mapeado a col 10
        ps.setString(11, e.getRutaCertElectronico());    // <-- Mapeado a col 11
        ps.setString(12, e.getRutaLog());                // <-- Mapeado a col 12
        ps.setString(13, e.getServicio());               // <-- Mapeado a col 13
        ps.setString(14, e.getDelegacion());             // <-- Mapeado a col 14
        if (e.getFechaAlta() == null || e.getFechaAlta().isBlank()) {
            ps.setNull(15, java.sql.Types.DATE);
        } else {
            ps.setString(15, e.getFechaAlta());          // <-- Mapeado a col 15
        }
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