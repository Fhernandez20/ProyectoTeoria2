package service;

import java.sql.*;
import java.util.*;

/**
 * MetadataExtractor — Consulta todas las system tables de MariaDB.
 *
 * LIMITACIONES DOCUMENTADAS (MariaDB):
 * - PAQUETES:    No existen en MariaDB. Son exclusivos de Oracle PL/SQL.
 * - TABLESPACES: MariaDB/InnoDB no expone tablespaces via system tables nativas
 *                sin information_schema. Se usa SHOW TABLE STATUS para datos
 *                de storage engine por tabla.
 * - SECUENCIAS:  Disponibles desde MariaDB 10.3 como objetos de primer nivel.
 *                Se listan usando mysql.proc (tipo='SEQUENCE' no existe) o
 *                mediante SHOW CREATE SEQUENCE. Se detectan via
 *                information_schema.TABLES WHERE TABLE_TYPE='SEQUENCE' — como
 *                esa vista está prohibida, se usa SHOW TABLES y se intenta
 *                SHOW CREATE SEQUENCE para cada candidato, filtrando errores.
 *                Alternativa limpia: SELECT table_name FROM
 *                mysql.tables_priv... tampoco aplica. La forma correcta sin
 *                information_schema es consultar directamente la tabla interna
 *                de MariaDB: SELECT * FROM mysql.sequence_table no existe.
 *                SOLUCIÓN ADOPTADA: Se intenta SHOW CREATE SEQUENCE <nombre>
 *                sobre cada objeto del catálogo para detectarlos, o bien el
 *                usuario las documenta manualmente. Ver sección Secuencias.
 */
public class MetadataExtractor {

    private final Connection conn;

    public MetadataExtractor(Connection conn) {
        this.conn = conn;
    }

    // =====================================================================
    // TABLAS
    // =====================================================================

    public List<String> listarTablas() {
        List<String> tablas = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) tablas.add(rs.getString(1));
        } catch (SQLException e) {
            System.err.println("[META] Error listando tablas: " + e.getMessage());
        }
        return tablas;
    }

    public String getDDLTabla(String nombre) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + nombre + "`")) {
            if (rs.next()) return rs.getString(2);
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "-- Sin resultado";
    }

    public Map<String, String> getColumnasTabla(String nombre) {
        Map<String, String> cols = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE `" + nombre + "`")) {
            while (rs.next()) {
                String col   = rs.getString("Field");
                String tipo  = rs.getString("Type");
                String nulo  = "YES".equals(rs.getString("Null")) ? "NULL" : "NOT NULL";
                String extra = rs.getString("Extra") != null ? rs.getString("Extra") : "";
                cols.put(col, tipo + " " + nulo + (extra.isBlank() ? "" : " " + extra));
            }
        } catch (SQLException e) {
            System.err.println("[META] Error columnas: " + e.getMessage());
        }
        return cols;
    }

    public String crearTabla(String nombre, List<String> columnas) {
        String sql = "CREATE TABLE `" + nombre + "` (\n"
                   + String.join(",\n", columnas) + "\n)";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return "Tabla creada: " + nombre;
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    // =====================================================================
    // VISTAS
    // =====================================================================

    public List<String> listarVistas() {
        List<String> vistas = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW FULL TABLES WHERE Table_type = 'VIEW'")) {
            while (rs.next()) vistas.add(rs.getString(1));
        } catch (SQLException e) {
            System.err.println("[META] Error listando vistas: " + e.getMessage());
        }
        return vistas;
    }

    public String getDDLVista(String nombre) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE VIEW `" + nombre + "`")) {
            if (rs.next()) return rs.getString(2);
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "-- Sin resultado";
    }

    public String crearVista(String nombre, String selectQuery) {
        String sql = "CREATE VIEW `" + nombre + "` AS " + selectQuery;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return "Vista creada: " + nombre;
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    // =====================================================================
    // PROCEDIMIENTOS ALMACENADOS
    // =====================================================================

    public List<String> listarProcedimientos() {
        List<String> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                 "SHOW PROCEDURE STATUS WHERE Db = ?")) {
            ps.setString(1, conn.getCatalog());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(rs.getString("Name"));
            }
        } catch (SQLException e) {
            System.err.println("[META] Error procedimientos: " + e.getMessage());
        }
        return lista;
    }

    public String getDDLProcedimiento(String nombre) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE PROCEDURE `" + nombre + "`")) {
            if (rs.next()) return rs.getString(3);
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "-- Sin resultado";
    }

    // =====================================================================
    // FUNCIONES
    // =====================================================================

    public List<String> listarFunciones() {
        List<String> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                 "SHOW FUNCTION STATUS WHERE Db = ?")) {
            ps.setString(1, conn.getCatalog());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(rs.getString("Name"));
            }
        } catch (SQLException e) {
            System.err.println("[META] Error funciones: " + e.getMessage());
        }
        return lista;
    }

    public String getDDLFuncion(String nombre) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE FUNCTION `" + nombre + "`")) {
            if (rs.next()) return rs.getString(3);
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "-- Sin resultado";
    }

    // =====================================================================
    // TRIGGERS
    // =====================================================================

    public List<Map<String, String>> listarTriggers() {
        List<Map<String, String>> lista = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TRIGGERS")) {
            while (rs.next()) {
                Map<String, String> t = new HashMap<>();
                t.put("nombre", rs.getString("Trigger"));
                t.put("tabla",  rs.getString("Table"));
                t.put("evento", rs.getString("Event"));
                t.put("timing", rs.getString("Timing"));
                lista.add(t);
            }
        } catch (SQLException e) {
            System.err.println("[META] Error triggers: " + e.getMessage());
        }
        return lista;
    }

    public String getDDLTrigger(String nombre) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TRIGGER `" + nombre + "`")) {
            if (rs.next()) return rs.getString(3);
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "-- Sin resultado";
    }

    // =====================================================================
    // ÍNDICES
    // =====================================================================

    /**
     * Lista todos los índices de TODAS las tablas de la base de datos activa.
     * Usa SHOW INDEX FROM <tabla> por cada tabla — system table nativa de MariaDB.
     */
    public List<Map<String, String>> listarTodosLosIndices() {
        List<Map<String, String>> indices = new ArrayList<>();
        for (String tabla : listarTablas()) {
            indices.addAll(listarIndicesDeTabla(tabla));
        }
        return indices;
    }

    public List<Map<String, String>> listarIndicesDeTabla(String tabla) {
        List<Map<String, String>> indices = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW INDEX FROM `" + tabla + "`")) {
            while (rs.next()) {
                Map<String, String> idx = new LinkedHashMap<>();
                idx.put("tabla",   tabla);
                idx.put("nombre",  rs.getString("Key_name"));
                idx.put("columna", rs.getString("Column_name"));
                idx.put("unique",  "0".equals(rs.getString("Non_unique")) ? "UNIQUE" : "");
                idx.put("tipo",    rs.getString("Index_type"));
                indices.add(idx);
            }
        } catch (SQLException e) {
            System.err.println("[META] Error indices de " + tabla + ": " + e.getMessage());
        }
        return indices;
    }

    /**
     * Genera el DDL de un índice a partir de SHOW INDEX FROM.
     * MariaDB no tiene SHOW CREATE INDEX — se reconstruye desde los metadatos.
     */
    public String getDDLIndice(String nombreIndice, String tabla) {
        // PRIMARY KEY no es un índice independiente
        if ("PRIMARY".equalsIgnoreCase(nombreIndice)) {
            return "-- PRIMARY KEY es parte del DDL de la tabla:\n\n"
                 + getDDLTabla(tabla);
        }

        StringBuilder sb = new StringBuilder();
        boolean unique = false;
        String tipo = "BTREE";
        List<String> columnas = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW INDEX FROM `" + tabla + "`")) {
            while (rs.next()) {
                if (nombreIndice.equals(rs.getString("Key_name"))) {
                    unique  = "0".equals(rs.getString("Non_unique"));
                    tipo    = rs.getString("Index_type");
                    columnas.add("`" + rs.getString("Column_name") + "`");
                }
            }
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }

        if (columnas.isEmpty()) return "-- Índice no encontrado";

        sb.append("CREATE ");
        if (unique) sb.append("UNIQUE ");
        sb.append("INDEX `").append(nombreIndice).append("`\n");
        sb.append("  USING ").append(tipo).append("\n");
        sb.append("  ON `").append(tabla).append("`\n");
        sb.append("  (").append(String.join(", ", columnas)).append(");");

        return sb.toString();
    }

    public String crearIndice(String tabla, String nombre, String columnas) {
        String sql = "CREATE INDEX `" + nombre + "` ON `" + tabla + "` (" + columnas + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return "Índice creado: " + nombre;
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    // =====================================================================
    // SECUENCIAS (MariaDB 10.3+)
    // =====================================================================

    /**
     * Lista secuencias en MariaDB 10.3+.
     *
     * JUSTIFICACIÓN TÉCNICA:
     * MariaDB no expone las secuencias en una system table propia sin pasar por
     * information_schema.TABLES (prohibido). La alternativa nativa es usar el
     * hecho de que las secuencias en MariaDB son objetos especiales que aparecen
     * en SHOW FULL TABLES con Table_type = 'SEQUENCE' (disponible en MariaDB 10.3+).
     * Esta es una consulta directa al catálogo del servidor, equivalente a las
     * system tables, sin usar information_schema.
     */
    public List<String> listarSecuencias() {
        List<String> lista = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW FULL TABLES WHERE Table_type = 'SEQUENCE'")) {
            while (rs.next()) lista.add(rs.getString(1));
        } catch (SQLException e) {
            // MariaDB < 10.3 no soporta secuencias
            System.err.println("[META] Secuencias no disponibles (requiere MariaDB 10.3+): "
                             + e.getMessage());
        }
        return lista;
    }

    public String getDDLSecuencia(String nombre) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE SEQUENCE `" + nombre + "`")) {
            if (rs.next()) return rs.getString(2);
        } catch (SQLException e) {
            return "-- Error (requiere MariaDB 10.3+): " + e.getMessage();
        }
        return "-- Sin resultado";
    }

    // =====================================================================
    // USUARIOS  (mysql.user — system table nativa)
    // =====================================================================

    public List<Map<String, String>> listarUsuarios() {
        List<Map<String, String>> lista = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT User, Host, plugin, password_expired " +
                 "FROM mysql.user ORDER BY User")) {
            while (rs.next()) {
                Map<String, String> u = new LinkedHashMap<>();
                u.put("usuario",  rs.getString("User"));
                u.put("host",     rs.getString("Host"));
                u.put("plugin",   rs.getString("plugin"));
                u.put("pwd_exp",  rs.getString("password_expired"));
                lista.add(u);
            }
        } catch (SQLException e) {
            System.err.println("[META] Error usuarios: " + e.getMessage());
        }
        return lista;
    }

    public String getDDLUsuario(String usuario, String host) {
        // SHOW CREATE USER es una sentencia nativa de MariaDB
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW CREATE USER `" + usuario + "`@`" + host + "`")) {
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return "-- Sin resultado";
    }

    public String getGrantsUsuario(String usuario, String host) {
        StringBuilder sb = new StringBuilder();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW GRANTS FOR `" + usuario + "`@`" + host + "`")) {
            while (rs.next()) {
                sb.append(rs.getString(1)).append(";\n");
            }
        } catch (SQLException e) {
            return "-- Error: " + e.getMessage();
        }
        return sb.toString();
    }

    // =====================================================================
    // TABLESPACES
    // =====================================================================

    /**
     * JUSTIFICACIÓN TABLESPACES EN MARIADB:
     * MariaDB/InnoDB no expone tablespaces a través de system tables nativas
     * accesibles sin information_schema. Las alternativas disponibles son:
     *   1. information_schema.INNODB_TABLESPACES (PROHIBIDO en este proyecto)
     *   2. SHOW TABLE STATUS — muestra datos de storage engine por tabla,
     *      incluyendo el engine (InnoDB, MyISAM, etc.) que implica el tablespace.
     *
     * SOLUCIÓN ADOPTADA: Se usa SHOW TABLE STATUS para mostrar información
     * relevante del storage engine y el data_length por tabla, que es la
     * información más cercana a "tablespace" disponible sin information_schema.
     */
    public List<Map<String, String>> listarTablespaces() {
        List<Map<String, String>> lista = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLE STATUS")) {
            while (rs.next()) {
                Map<String, String> ts = new LinkedHashMap<>();
                ts.put("tabla",   rs.getString("Name"));
                ts.put("engine",  rs.getString("Engine"));
                ts.put("rows",    rs.getString("Rows"));
                String dl = rs.getString("Data_length");
                ts.put("data_length", dl != null ? dl : "0");
                ts.put("row_format", rs.getString("Row_format"));
                lista.add(ts);
            }
        } catch (SQLException e) {
            System.err.println("[META] Error tablespaces: " + e.getMessage());
        }
        return lista;
    }

    // =====================================================================
    // EJECUCIÓN DE SQL
    // =====================================================================

    public List<Map<String, String>> ejecutarSelectRetornarMapa(String sql) throws SQLException {
        List<Map<String, String>> resultados = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, String> fila = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String val = rs.getString(i);
                    fila.put(meta.getColumnLabel(i), val != null ? val : "NULL");
                }
                resultados.add(fila);
            }
        }
        return resultados;
    }

    public int ejecutarDML(String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }
}
