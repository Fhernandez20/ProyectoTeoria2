package service;

import java.sql.*;
import java.util.*;

public class MetadataExtractor {

    private Connection conn;

    public MetadataExtractor(Connection conn) {
        this.conn = conn;
    }

    // ========== TABLA ==========
    public String getDDLTabla(String nombreTabla) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + nombreTabla)) {
            if (rs.next()) {
                return rs.getString(2);
            }
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "";
    }

    public List<String> listarTablas() {
        List<String> tablas = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) {
                tablas.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Error listando tablas: " + e.getMessage());
        }
        return tablas;
    }

    public Map<String, String> getColumnasTabla(String nombreTabla) {
        Map<String, String> columnas = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + nombreTabla)) {
            while (rs.next()) {
                String nombre = rs.getString("Field");
                String tipo = rs.getString("Type");
                String nulo = "YES".equals(rs.getString("Null")) ? "NULL" : "NOT NULL";
                String extra = rs.getString("Extra") != null ? rs.getString("Extra") : "";
                columnas.put(nombre, tipo + " " + nulo + " " + extra);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return columnas;
    }

    // ========== VISTA ==========
    public String getDDLVista(String nombreVista) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE VIEW " + nombreVista)) {
            if (rs.next()) {
                return rs.getString(2);
            }
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "";
    }

    public List<String> listarVistas() {
        List<String> vistas = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW FULL TABLES WHERE Table_type = 'VIEW'")) {
            while (rs.next()) {
                vistas.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Error listando vistas: " + e.getMessage());
        }
        return vistas;
    }

    // ========== PROCEDIMIENTO ==========
    public String getDDLProcedimiento(String nombreProc) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE PROCEDURE " + nombreProc)) {
            if (rs.next()) {
                return rs.getString(3);
            }
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "";
    }

    public List<String> listarProcedimientos() {
        List<String> procs = new ArrayList<>();
        try {
            String bd = conn.getCatalog();
            PreparedStatement ps = conn.prepareStatement("SHOW PROCEDURE STATUS WHERE Db = ?");
            ps.setString(1, bd);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                procs.add(rs.getString("Name"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return procs;
    }

    // ========== FUNCION ==========
    public String getDDLFuncion(String nombreFunc) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE FUNCTION " + nombreFunc)) {
            if (rs.next()) {
                return rs.getString(3);
            }
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "";
    }

    public List<String> listarFunciones() {
        List<String> funcs = new ArrayList<>();
        try {
            String bd = conn.getCatalog();
            PreparedStatement ps = conn.prepareStatement("SHOW FUNCTION STATUS WHERE Db = ?");
            ps.setString(1, bd);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                funcs.add(rs.getString("Name"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return funcs;
    }

    // ========== TRIGGER ==========
    public String getDDLTrigger(String nombreTrigger) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TRIGGER " + nombreTrigger)) {
            if (rs.next()) {
                return rs.getString(3);
            }
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "";
    }

    public List<Map<String, String>> listarTriggers() {
        List<Map<String, String>> triggers = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TRIGGERS")) {
            while (rs.next()) {
                Map<String, String> trigger = new HashMap<>();
                trigger.put("nombre", rs.getString("Trigger"));
                trigger.put("tabla", rs.getString("Table"));
                trigger.put("evento", rs.getString("Event"));
                trigger.put("timing", rs.getString("Timing"));
                triggers.add(trigger);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return triggers;
    }

    // ========== INDICES ==========
    public List<Map<String, String>> listarIndices(String nombreTabla) {
        List<Map<String, String>> indices = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW INDEX FROM " + nombreTabla)) {
            while (rs.next()) {
                Map<String, String> idx = new HashMap<>();
                idx.put("nombre", rs.getString("Key_name"));
                idx.put("columna", rs.getString("Column_name"));
                idx.put("unique", "0".equals(rs.getString("Non_unique")) ? "UNIQUE" : "");
                indices.add(idx);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return indices;
    }

    public String crearIndice(String nombreTabla, String nombreIndice, String columnas) {
        String sql = "CREATE INDEX " + nombreIndice + " ON " + nombreTabla + " (" + columnas + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return "Indice creado exitosamente";
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    // ========== EJECUCION DE SQL ==========
    public ResultSet ejecutarSelect(String sql) throws SQLException {
        return conn.createStatement().executeQuery(sql);
    }

    public int ejecutarDML(String sql) throws SQLException {
        return conn.createStatement().executeUpdate(sql);
    }

    public List<Map<String, String>> ejecutarSelectRetornarMapa(String sql) {
        List<Map<String, String>> resultados = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, String> fila = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String valor = rs.getString(i);
                    fila.put(meta.getColumnName(i), valor != null ? valor : "NULL");
                }
                resultados.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resultados;
    }

    // ========== CREAR TABLA DESDE PARAMETROS ==========
    public String crearTabla(String nombre, List<String> columnas) {
        StringBuilder sql = new StringBuilder("CREATE TABLE " + nombre + " (\n");
        sql.append(String.join(",\n", columnas));
        sql.append("\n)");

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
            return "Tabla creada: " + nombre;
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    // ========== CREAR VISTA ==========
    public String crearVista(String nombre, String selectQuery) {
        String sql = "CREATE VIEW " + nombre + " AS " + selectQuery;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return "Vista creada: " + nombre;
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }
}
