package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import model.ConexionGuardada;
import service.ConexionManager;
import service.MetadataExtractor;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    @FXML private Label lblConexionActiva;
    @FXML private Label lblEstadoBD;
    @FXML private Label lblStatus;
    @FXML private TreeView<String> treeObjetos;
    @FXML private TabPane tabPane;

    private MetadataExtractor metadata;
    private String objetoSeleccionado = "";
    private String tipoObjeto = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ConexionGuardada datos = ConexionManager.getDatosActivos();
        if (datos != null) {
            lblConexionActiva.setText(datos.getUsuario() + "@" + datos.getHost()
                + ":" + datos.getPuerto());
            lblEstadoBD.setText("Conectado a: " + datos.getBaseDatos());
        }

        metadata = new MetadataExtractor(ConexionManager.getConexion());

        // Crear tabs
        crearTabDDL();
        crearTabEditorVisual();
        crearTabSQLExecutor();

        cargarArbolObjetos();
    }

    // ================================================================
    // ARBOL DE OBJETOS
    // ================================================================
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
        treeObjetos.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, nueva) -> {
                if (nueva != null) {
                    objetoSeleccionado = nueva.getValue();
                    detectarTipoObjeto();
                }
            }
        );

        lblStatus.setText("Objetos cargados correctamente.");
    }

    private void detectarTipoObjeto() {
        if (metadata == null) return;

        if (metadata.listarTablas().contains(objetoSeleccionado)) {
            tipoObjeto = "TABLA";
            lblStatus.setText("Tabla seleccionada: " + objetoSeleccionado);
        } else if (metadata.listarVistas().contains(objetoSeleccionado)) {
            tipoObjeto = "VISTA";
            lblStatus.setText("Vista seleccionada: " + objetoSeleccionado);
        } else if (metadata.listarProcedimientos().contains(objetoSeleccionado)) {
            tipoObjeto = "PROCEDIMIENTO";
            lblStatus.setText("Procedimiento seleccionado: " + objetoSeleccionado);
        } else if (metadata.listarFunciones().contains(objetoSeleccionado)) {
            tipoObjeto = "FUNCION";
            lblStatus.setText("Funcion seleccionada: " + objetoSeleccionado);
        } else {
            tipoObjeto = "";
        }
    }

    private void cargarBasesDeDatos(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Bases de datos");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("Error: " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarTablas(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Tablas");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("Error: " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarVistas(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Vistas");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SHOW FULL TABLES WHERE Table_type = 'VIEW'")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("(ninguna)"));
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarProcedimientos(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Procedimientos");
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
            nodo.getChildren().add(new TreeItem<>("Error: " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarFunciones(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Funciones");
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
            nodo.getChildren().add(new TreeItem<>("Error: " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarTriggers(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Triggers");
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
            nodo.getChildren().add(new TreeItem<>("Error: " + e.getMessage()));
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarUsuarios(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Usuarios");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT User, Host FROM mysql.user ORDER BY User")) {
            while (rs.next()) {
                nodo.getChildren().add(
                    new TreeItem<>(rs.getString("User") + "@" + rs.getString("Host")));
            }
        } catch (SQLException e) {
            nodo.getChildren().add(new TreeItem<>("(sin permisos)"));
        }
        raiz.getChildren().add(nodo);
    }

    // ================================================================
    // TAB 1: DDL VIEWER
    // ================================================================
    private void crearTabDDL() {
        Tab tabDDL = new Tab();
        tabDDL.setText("DDL");
        tabDDL.setClosable(false);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(16));

        Label lblTitulo = new Label("SQL de creacion del objeto seleccionado:");
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        TextArea textDDL = new TextArea();
        textDDL.setWrapText(false);
        textDDL.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        textDDL.setEditable(false);
        VBox.setVgrow(textDDL, Priority.ALWAYS);

        HBox btnBox = new HBox(10);
        Button btnCargar = new Button("Cargar DDL");
        btnCargar.setStyle("-fx-padding: 8px 16px; -fx-font-size: 12px;");
        btnCargar.setOnAction(e -> {
            if (tipoObjeto.isEmpty()) {
                textDDL.setText("Selecciona un objeto del arbol.");
                return;
            }
            String ddl = obtenerDDL();
            textDDL.setText(ddl);
            lblStatus.setText("DDL cargado para: " + objetoSeleccionado);
        });

        Button btnCopiar = new Button("Copiar");
        btnCopiar.setStyle("-fx-padding: 8px 16px; -fx-font-size: 12px;");
        btnCopiar.setOnAction(e -> {
            String contenido = textDDL.getText();
            if (!contenido.isEmpty()) {
                javafx.scene.input.Clipboard clip = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(contenido);
                clip.setContent(content);
                lblStatus.setText("DDL copiado al portapapeles");
            }
        });

        btnBox.getChildren().addAll(btnCargar, btnCopiar);

        vbox.getChildren().addAll(lblTitulo, textDDL, btnBox);
        tabDDL.setContent(vbox);

        if (tabPane != null) {
            tabPane.getTabs().add(tabDDL);
        }
    }

    private String obtenerDDL() {
        switch (tipoObjeto) {
            case "TABLA":
                return metadata.getDDLTabla(objetoSeleccionado);
            case "VISTA":
                return metadata.getDDLVista(objetoSeleccionado);
            case "PROCEDIMIENTO":
                return metadata.getDDLProcedimiento(objetoSeleccionado);
            case "FUNCION":
                return metadata.getDDLFuncion(objetoSeleccionado);
            default:
                return "";
        }
    }

    // ================================================================
    // TAB 2: EDITOR VISUAL
    // ================================================================
    private void crearTabEditorVisual() {
        Tab tabEditor = new Tab();
        tabEditor.setText("Editor Visual");
        tabEditor.setClosable(false);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(16));

        Label lblTitulo = new Label("Crear nueva tabla:");
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        HBox hboxNombre = new HBox(10);
        Label lblNombre = new Label("Nombre tabla:");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("usuarios");
        hboxNombre.getChildren().addAll(lblNombre, txtNombre);

        Label lblColsTitle = new Label("Columnas (nombre tipo [NULL|NOT NULL]):");
        lblColsTitle.setStyle("-fx-font-size: 11px;");

        TextArea textColsInput = new TextArea();
        textColsInput.setPrefRowCount(8);
        textColsInput.setWrapText(true);
        textColsInput.setText("id INT AUTO_INCREMENT PRIMARY KEY,\nnombre VARCHAR(100) NOT NULL,\nemail VARCHAR(100) UNIQUE,\nfecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        VBox.setVgrow(textColsInput, Priority.ALWAYS);

        HBox btnBox = new HBox(10);
        Button btnCrear = new Button("Crear Tabla");
        btnCrear.setStyle("-fx-padding: 8px 16px;");
        btnCrear.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                lblStatus.setText("Ingresa un nombre para la tabla");
                return;
            }
            List<String> columnas = Arrays.stream(textColsInput.getText().split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

            String resultado = metadata.crearTabla(nombre, columnas);
            lblStatus.setText(resultado);

            if (resultado.startsWith("Tabla creada")) {
                txtNombre.clear();
                textColsInput.clear();
                cargarArbolObjetos();
            }
        });

        btnBox.getChildren().add(btnCrear);
        vbox.getChildren().addAll(lblTitulo, hboxNombre, lblColsTitle, textColsInput, btnBox);
        tabEditor.setContent(vbox);

        if (tabPane != null) {
            tabPane.getTabs().add(tabEditor);
        }
    }

    // ================================================================
    // TAB 3: SQL EXECUTOR
    // ================================================================
    private void crearTabSQLExecutor() {
        Tab tabSQL = new Tab();
        tabSQL.setText("Ejecutor SQL");
        tabSQL.setClosable(false);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(16));

        Label lblTitulo = new Label("Ejecuta SELECT o comandos DML/DDL:");
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        TextArea textSQL = new TextArea();
        textSQL.setPrefRowCount(6);
        textSQL.setWrapText(true);
        textSQL.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        textSQL.setText("SELECT * FROM ");
        VBox.setVgrow(textSQL, Priority.ALWAYS);

        HBox btnBox = new HBox(10);
        Button btnEjecutar = new Button("Ejecutar");
        btnEjecutar.setStyle("-fx-padding: 8px 16px;");
        btnEjecutar.setOnAction(e -> {
            String sql = textSQL.getText().trim();
            if (sql.isEmpty()) {
                lblStatus.setText("Ingresa una sentencia SQL");
                return;
            }
            ejecutarSQL(sql, vbox);
        });
        btnBox.getChildren().add(btnEjecutar);

        vbox.getChildren().addAll(lblTitulo, textSQL, btnBox);
        tabSQL.setContent(vbox);

        if (tabPane != null) {
            tabPane.getTabs().add(tabSQL);
        }
    }

    private void ejecutarSQL(String sql, VBox contenedor) {
        new Thread(() -> {
            try {
                if (sql.trim().toLowerCase().startsWith("select")) {
                    List<Map<String, String>> resultados = metadata.ejecutarSelectRetornarMapa(sql);
                    javafx.application.Platform.runLater(() -> {
                        mostrarResultados(resultados, contenedor);
                        lblStatus.setText("SELECT ejecutado (" + resultados.size() + " filas)");
                    });
                } else {
                    int filas = metadata.ejecutarDML(sql);
                    javafx.application.Platform.runLater(() -> {
                        lblStatus.setText("Comando ejecutado (" + filas + " filas afectadas)");
                        cargarArbolObjetos();
                    });
                }
            } catch (SQLException ex) {
                javafx.application.Platform.runLater(() ->
                    lblStatus.setText("Error: " + ex.getMessage())
                );
            }
        }).start();
    }

    private void mostrarResultados(List<Map<String, String>> resultados, VBox contenedor) {
        if (resultados.isEmpty()) {
            lblStatus.setText("Sin resultados");
            return;
        }

        // Limpiar tabla anterior si existe
        ObservableList<Node> children = contenedor.getChildren();
        if (children.size() > 4) {
            children.remove(4, children.size());
        }

        // Crear TableView dinamicamente
        TableView<Map<String, String>> tabla = new TableView<>();
        Set<String> columnas = resultados.get(0).keySet();

        for (String colNombre : columnas) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(colNombre);
            col.setCellValueFactory(cellData -> {
                Map<String, String> fila = cellData.getValue();
                String valor = fila.getOrDefault(colNombre, "");
                return new SimpleStringProperty(valor);
            });
            col.setPrefWidth(120);
            tabla.getColumns().add(col);
        }

        ObservableList<Map<String, String>> datos = FXCollections.observableArrayList(resultados);
        tabla.setItems(datos);
        tabla.setPrefHeight(250);

        contenedor.getChildren().add(tabla);
    }

    // ================================================================
    // BOTONES PRINCIPALES
    // ================================================================
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
            lblStatus.setText("Error: " + e.getMessage());
        }
    }
}
