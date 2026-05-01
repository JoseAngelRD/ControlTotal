package com.example.controltotal_proyecto.bd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona la conexión única a la base de datos MySQL.
 * Ajusta URL, USER y PASSWORD según tu entorno.
 */
public class ConexionBD {

    private static final String URL      = "jdbc:mysql://127.0.0.1:3306/control_total_bd";
    private static final String USER     = "root";
    private static final String PASSWORD = "root";

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("❌ No se pudo conectar a la base de datos: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

