package service;

import model.ConexionGuardada;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionManager {

    private static Connection conexionActiva = null;
    private static ConexionGuardada datosActivos = null;


    public static boolean conectar(ConexionGuardada datos) {
        try {
            cerrar();

            Class.forName("org.mariadb.jdbc.Driver");

            Connection conn = DriverManager.getConnection(
                datos.buildJdbcUrl(),
                datos.getUsuario(),
                datos.getContrasena()
            );

            conexionActiva = conn;
            datosActivos = datos;
            return true;

        } catch (ClassNotFoundException e) {
            System.err.println("Driver MariaDB no encontrado: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.err.println("Error al conectar: " + e.getMessage());
            return false;
        }
    }

    public static String probarConexion(ConexionGuardada datos) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            Connection prueba = DriverManager.getConnection(
                datos.buildJdbcUrl(),
                datos.getUsuario(),
                datos.getContrasena()
            );
            prueba.close();
            return null; 
        } catch (ClassNotFoundException e) {
            return "Driver MariaDB no encontrado. Verifica el JAR en /lib";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public static Connection getConexion() {
        return conexionActiva;
    }

    public static ConexionGuardada getDatosActivos() {
        return datosActivos;
    }

    public static boolean estaConectado() {
        try {
            return conexionActiva != null && !conexionActiva.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void cerrar() {
        if (conexionActiva != null) {
            try {
                conexionActiva.close();
            } catch (SQLException ignored) {}
            conexionActiva = null;
            datosActivos = null;
        }
    }
}
