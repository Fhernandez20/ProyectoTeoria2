package model;


public class ConexionGuardada {

    private String alias;      
    private String host;
    private int puerto;
    private String usuario;
    private String contrasena;  
    private String baseDatos;  

    public ConexionGuardada() {}

    public ConexionGuardada(String alias, String host, int puerto,
                             String usuario, String contrasena, String baseDatos) {
        this.alias = alias;
        this.host = host;
        this.puerto = puerto;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.baseDatos = baseDatos;
    }


    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPuerto() { return puerto; }
    public void setPuerto(int puerto) { this.puerto = puerto; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getBaseDatos() { return baseDatos; }
    public void setBaseDatos(String baseDatos) { this.baseDatos = baseDatos; }

    public String buildJdbcUrl() {
        String db = (baseDatos != null && !baseDatos.isBlank()) ? baseDatos : "mysql";
        return "jdbc:mariadb://" + host + ":" + puerto + "/" + db;
    }

    @Override
    public String toString() {
        return alias + " (" + usuario + "@" + host + ":" + puerto + ")";
    }
}
