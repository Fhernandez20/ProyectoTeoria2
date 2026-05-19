package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.ConexionGuardada;
import service.ConexionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;


public class MainController implements Initializable {

    @FXML private Label lblConexionActiva;
    @FXML private Label lblEstadoBD;
    @FXML private Label lblStatus;
    @FXML private TreeView<String> treeObjetos;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ConexionGuardada datos = ConexionManager.getDatosActivos();
        if (datos != null) {
            lblConexionActiva.setText(datos.getUsuario() + "@" + datos.getHost()
                + ":" + datos.getPuerto());
            lblEstadoBD.setText("Conectado a: " + datos.getBaseDatos());
        }

        cargarArbolObjetos();
    }

    private void cargarArbolObjetos() {
        Connection conn = ConexionManager.getConexion();
        if (conn == null) return;

        TreeItem<String> raiz = new TreeItem<>("Servidor");
        raiz.setExpanded(true);

        cargarBasesDeDatos(conn, raiz);
        cargarTablas(conn, raiz);
        cargarVistas(conn, raiz);
        cargarProcedimientos(conn, raiz);
        cargarFunciones(conn, raiz);
        cargarTriggers(conn, raiz);
        cargarUsuarios(conn, raiz);

        treeObjetos.setRoot(raiz);
        lblStatus.setText("Objetos cargados correctamente.");
    }

    // ---------------------------------------------------------------
    // BASES DE DATOS  
    // ---------------------------------------------------------------
    private void cargarBasesDeDatos(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("📁 Bases de datos");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("⚠ " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    // ---------------------------------------------------------------
    // TABLAS  
    // ---------------------------------------------------------------
    private void cargarTablas(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("📋 Tablas");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("⚠ " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    // ---------------------------------------------------------------
    // VISTAS  
    // ---------------------------------------------------------------
    private void cargarVistas(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("👁 Vistas");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW FULL TABLES WHERE Table_type = 'VIEW'")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        } catch (SQLException e) {
            // Si no hay vistas o no hay permisos, nodo vacío
            nodo.getChildren().add(new TreeItem<>("(ninguna)"));
        }
        raiz.getChildren().add(nodo);
    }

    // ---------------------------------------------------------------
    // PROCEDIMIENTOS  
    // ---------------------------------------------------------------
    private void cargarProcedimientos(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("⚙ Procedimientos");
        try {
            String bd = conn.getCatalog();
            PreparedStatement ps = conn.prepareStatement(
                "SHOW PROCEDURE STATUS WHERE Db = ?");
            ps.setString(1, bd);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString("Name")));
            }
            if (nodo.getChildren().isEmpty()) {
                nodo.getChildren().add(new TreeItem<>("(ninguno)"));
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("⚠ " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    // ---------------------------------------------------------------
    // FUNCIONES  
    // ---------------------------------------------------------------
    private void cargarFunciones(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("𝑓 Funciones");
        try {
            String bd = conn.getCatalog();
            PreparedStatement ps = conn.prepareStatement(
                "SHOW FUNCTION STATUS WHERE Db = ?");
            ps.setString(1, bd);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString("Name")));
            }
            if (nodo.getChildren().isEmpty()) {
                nodo.getChildren().add(new TreeItem<>("(ninguna)"));
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("⚠ " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    // ---------------------------------------------------------------
    // TRIGGERS  
    // ---------------------------------------------------------------
    private void cargarTriggers(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("⚡ Triggers");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TRIGGERS")) {
            while (rs.next()) {
                String nombre = rs.getString("Trigger");
                String tabla  = rs.getString("Table");
                String evento = rs.getString("Event");
                nodo.getChildren().add(
                    new TreeItem<>(nombre + " [" + evento + " ON " + tabla + "]"));
            }
            if (nodo.getChildren().isEmpty()) {
                nodo.getChildren().add(new TreeItem<>("(ninguno)"));
            }
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("⚠ " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    // ---------------------------------------------------------------
    // USUARIOS  
    // ---------------------------------------------------------------
    private void cargarUsuarios(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("👤 Usuarios");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT User, Host FROM mysql.user ORDER BY User")) {
            while (rs.next()) {
                nodo.getChildren().add(
                    new TreeItem<>(rs.getString("User") + "@" + rs.getString("Host")));
            }
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("(sin permisos para ver usuarios)"));
        }
        raiz.getChildren().add(nodo);
    }

    // ---------------------------------------------------------------
    // ACCIONES DE BOTONES
    // ---------------------------------------------------------------

    @FXML
    private void onRefrescar() {
        lblStatus.setText("Recargando...");
        cargarArbolObjetos();
    }

    @FXML
    private void onDesconectar() {
        ConexionManager.cerrar();
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Database Manager Tool");
            stage.setScene(new Scene(root, 520, 600));
            stage.getScene().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());
            stage.show();

            Stage mainStage = (Stage) lblStatus.getScene().getWindow();
            mainStage.close();

        } catch (IOException e) {
            lblStatus.setText("Error al volver al login: " + e.getMessage());
        }
    }
}
