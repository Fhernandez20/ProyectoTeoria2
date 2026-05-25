package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.Node;
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

    @FXML public Label lblConexionActiva;
    @FXML public Label lblEstadoBD;
    @FXML public Label lblStatus;
    @FXML public TreeView<String> treeObjetos;
    @FXML public TabPane tabPane;

    private MetadataExtractor metadata;
    private String objetoSeleccionado = "";
    private String tipoObjeto = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("[INIT] Iniciando MainController...");
        
        ConexionGuardada datos = ConexionManager.getDatosActivos();
        if (datos != null) {
            lblConexionActiva.setText(datos.getUsuario() + "@" + datos.getHost()
                + ":" + datos.getPuerto());
            lblEstadoBD.setText("BD: " + datos.getBaseDatos());
        }

        Connection conn = ConexionManager.getConexion();
        if (conn == null) {
            lblStatus.setText("ERROR: No hay conexion activa");
            return;
        }

        metadata = new MetadataExtractor(conn);
        
        System.out.println("[INIT] Creando tabs...");
        crearTabDDL();
        crearTabEditorVisual();
        crearTabSQLExecutor();
        
        System.out.println("[INIT] Cargando arbol de objetos...");
        cargarArbolObjetos();
        
        System.out.println("[INIT] MainController iniciado correctamente");
    }

    // ================================================================
    // CARGAR ARBOL
    // ================================================================
    private void cargarArbolObjetos() {
        Connection conn = ConexionManager.getConexion();
        if (conn == null) return;

        TreeItem<String> raiz = new TreeItem<>("Servidor");
        raiz.setExpanded(true);

        try {
            cargarTablas(conn, raiz);
            cargarVistas(conn, raiz);
            cargarProcedimientos(conn, raiz);
            cargarFunciones(conn, raiz);
            cargarTriggers(conn, raiz);
            cargarUsuarios(conn, raiz);
        } catch (Exception e) {
            System.err.println("Error cargando arbol: " + e.getMessage());
            e.printStackTrace();
        }

        treeObjetos.setRoot(raiz);
        treeObjetos.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, nueva) -> {
                if (nueva != null) {
                    String texto = nueva.getValue();
                    if (!texto.contains("(") && !texto.isEmpty()) {
                        objetoSeleccionado = texto;
                        detectarTipoObjeto();
                    }
                }
            }
        );

        lblStatus.setText("Objetos cargados");
    }

    private void detectarTipoObjeto() {
        if (metadata == null) return;
        
        if (metadata.listarTablas().contains(objetoSeleccionado)) {
            tipoObjeto = "TABLA";
        } else if (metadata.listarVistas().contains(objetoSeleccionado)) {
            tipoObjeto = "VISTA";
        } else if (metadata.listarProcedimientos().contains(objetoSeleccionado)) {
            tipoObjeto = "PROCEDIMIENTO";
        } else if (metadata.listarFunciones().contains(objetoSeleccionado)) {
            tipoObjeto = "FUNCION";
        } else if (metadata.listarTriggers().stream()
                   .anyMatch(t -> t.get("nombre").equals(objetoSeleccionado))) {
            tipoObjeto = "TRIGGER";
        } else {
            tipoObjeto = "";
        }
    }

    private void cargarTablas(Connection conn, TreeItem<String> raiz) throws SQLException {
        TreeItem<String> nodo = new TreeItem<>("Tablas");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarVistas(Connection conn, TreeItem<String> raiz) throws SQLException {
        TreeItem<String> nodo = new TreeItem<>("Vistas");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW FULL TABLES WHERE Table_type = 'VIEW'")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarProcedimientos(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Procedimientos");
        try {
            String bd = conn.getCatalog();
            PreparedStatement ps = conn.prepareStatement("SHOW PROCEDURE STATUS WHERE Db = ?");
            ps.setString(1, bd);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString("Name")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error cargando procedimientos: " + e.getMessage());
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarFunciones(Connection conn, TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Funciones");
        try {
            String bd = conn.getCatalog();
            PreparedStatement ps = conn.prepareStatement("SHOW FUNCTION STATUS WHERE Db = ?");
            ps.setString(1, bd);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString("Name")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error cargando funciones: " + e.getMessage());
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarTriggers(Connection conn, TreeItem<String> raiz) throws SQLException {
        TreeItem<String> nodo = new TreeItem<>("Triggers");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TRIGGERS")) {
            while (rs.next()) {
                String nombre = rs.getString("Trigger");
                String tabla = rs.getString("Table");
                String evento = rs.getString("Event");
                nodo.getChildren().add(new TreeItem<>(nombre));
            }
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarUsuarios(Connection conn, TreeItem<String> raiz) throws SQLException {
        TreeItem<String> nodo = new TreeItem<>("Usuarios");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT User, Host FROM mysql.user ORDER BY User")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString("User") + "@" + rs.getString("Host")));
            }
        }
        raiz.getChildren().add(nodo);
    }

    // ================================================================
    // TAB 1: DDL
    // ================================================================
    private void crearTabDDL() {
        Tab tabDDL = new Tab();
        tabDDL.setText("DDL");
        tabDDL.setClosable(false);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(16));
        vbox.setStyle("-fx-background-color: #0f1117;");

        Label lblTitulo = new Label("Ver SQL de creacion:");
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        TextArea textDDL = new TextArea();
        textDDL.setWrapText(false);
        textDDL.setEditable(false);
        aplicarEstiloDarkTextArea(textDDL);
        VBox.setVgrow(textDDL, Priority.ALWAYS);

        Button btnCargar = new Button("Cargar DDL");
        btnCargar.setStyle("-fx-padding: 8px 16px;");
        btnCargar.setOnAction(e -> {
            if (tipoObjeto.isEmpty()) {
                textDDL.setText("Selecciona un objeto del arbol");
                return;
            }
            String ddl = obtenerDDL();
            textDDL.setText(ddl);
            lblStatus.setText("DDL cargado: " + objetoSeleccionado);
        });

        vbox.getChildren().addAll(lblTitulo, textDDL, btnCargar);
        tabDDL.setContent(vbox);
        tabPane.getTabs().add(tabDDL);
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
            case "TRIGGER":
                return metadata.getDDLTrigger(objetoSeleccionado);
            default:
                return "Selecciona una tabla, vista, procedimiento, función o trigger";
        }
    }

    // ================================================================
    // TAB 2: EDITOR VISUAL
    // ================================================================
    private void crearTabEditorVisual() {
        Tab tabEditor = new Tab();
        tabEditor.setText("Editor Visual");
        tabEditor.setClosable(false);

        ScrollPane scroll = new ScrollPane();
        scroll.setStyle("-fx-background-color: #0f1117; -fx-border-color: transparent;");
        scroll.setFitToWidth(true);

        VBox mainVBox = new VBox(20);
        mainVBox.setPadding(new Insets(20));
        mainVBox.setStyle("-fx-background-color: #0f1117;");

        // ========== SECCIÓN DE TABLAS ==========
        VBox sectionTabla = crearSeccionTabla();
        
        // ========== SECCIÓN DE VISTAS ==========
        VBox sectionVista = crearSeccionVista();

        mainVBox.getChildren().addAll(sectionTabla, sectionVista);
        scroll.setContent(mainVBox);
        tabEditor.setContent(scroll);
        tabPane.getTabs().add(tabEditor);
    }

    private VBox crearSeccionTabla() {
        VBox section = new VBox(10);
        section.setStyle(
            "-fx-border-color: #4ade80; " +
            "-fx-border-width: 2; " +
            "-fx-padding: 15; " +
            "-fx-border-radius: 6; " +
            "-fx-background-color: #0d1b0f;"
        );

        // Título
        Label lblTitulo = new Label("📋 Crear Nueva Tabla");
        lblTitulo.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #4ade80; " +
            "-fx-letter-spacing: 1;"
        );

        // Nombre
        HBox hboxNombre = new HBox(10);
        hboxNombre.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblNombre = new Label("Nombre tabla:");
        lblNombre.setStyle("-fx-text-fill: #94a3b8; -fx-min-width: 120;");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("ej: usuarios");
        txtNombre.setStyle("-fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0; -fx-padding: 8;");
        txtNombre.setPrefWidth(300);
        hboxNombre.getChildren().addAll(lblNombre, txtNombre);

        // Columnas
        Label lblCols = new Label("Definición de columnas:");
        lblCols.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        TextArea textCols = new TextArea();
        textCols.setPrefRowCount(5);
        textCols.setWrapText(true);
        textCols.setText("id INT AUTO_INCREMENT PRIMARY KEY,\nnombre VARCHAR(100) NOT NULL,\nemail VARCHAR(100) UNIQUE");
        textCols.setStyle(
            "-fx-font-family: 'Courier New'; " +
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1a1d27; " +
            "-fx-text-fill: #4ade80; " +
            "-fx-control-inner-background: #1a1d27; " +
            "-fx-border-color: #4ade80; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 8;"
        );
        VBox.setVgrow(textCols, Priority.SOMETIMES);

        // Botón
        Button btnCrear = new Button("✓ Crear Tabla");
        btnCrear.setStyle(
            "-fx-background-color: #4ade80; " +
            "-fx-text-fill: #0f1117; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-cursor: hand;"
        );
        btnCrear.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                lblStatus.setText("❌ ERROR: Ingresa el nombre de la tabla");
                return;
            }
            List<String> columnas = Arrays.stream(textCols.getText().split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

            if (columnas.isEmpty()) {
                lblStatus.setText("❌ ERROR: Ingresa al menos una columna");
                return;
            }

            String resultado = metadata.crearTabla(nombre, columnas);
            lblStatus.setText(resultado);

            if (resultado.startsWith("Tabla creada")) {
                txtNombre.clear();
                textCols.clear();
                cargarArbolObjetos();
            }
        });

        section.getChildren().addAll(lblTitulo, hboxNombre, lblCols, textCols, btnCrear);
        return section;
    }

    private VBox crearSeccionVista() {
        VBox section = new VBox(10);
        section.setStyle(
            "-fx-border-color: #22d3ee; " +
            "-fx-border-width: 2; " +
            "-fx-padding: 15; " +
            "-fx-border-radius: 6; " +
            "-fx-background-color: #0a1929;"
        );

        // Título
        Label lblTitulo = new Label("👁️ Crear Nueva Vista");
        lblTitulo.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #22d3ee; " +
            "-fx-letter-spacing: 1;"
        );

        // Nombre
        HBox hboxNombre = new HBox(10);
        hboxNombre.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblNombre = new Label("Nombre vista:");
        lblNombre.setStyle("-fx-text-fill: #94a3b8; -fx-min-width: 120;");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("ej: vista_usuarios");
        txtNombre.setStyle("-fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0; -fx-padding: 8;");
        txtNombre.setPrefWidth(300);
        hboxNombre.getChildren().addAll(lblNombre, txtNombre);

        // SELECT
        Label lblSelect = new Label("Consulta SELECT:");
        lblSelect.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        TextArea textSelect = new TextArea();
        textSelect.setPrefRowCount(5);
        textSelect.setWrapText(true);
        textSelect.setText("SELECT id, nombre, email FROM usuarios WHERE estado = 1");
        textSelect.setStyle(
            "-fx-font-family: 'Courier New'; " +
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1a1d27; " +
            "-fx-text-fill: #22d3ee; " +
            "-fx-control-inner-background: #1a1d27; " +
            "-fx-border-color: #22d3ee; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 8;"
        );
        VBox.setVgrow(textSelect, Priority.SOMETIMES);

        // Botón
        Button btnCrear = new Button("✓ Crear Vista");
        btnCrear.setStyle(
            "-fx-background-color: #22d3ee; " +
            "-fx-text-fill: #0f1117; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-cursor: hand;"
        );
        btnCrear.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                lblStatus.setText("❌ ERROR: Ingresa el nombre de la vista");
                return;
            }
            String select = textSelect.getText().trim();
            if (select.isEmpty()) {
                lblStatus.setText("❌ ERROR: Ingresa la consulta SELECT");
                return;
            }

            String resultado = metadata.crearVista(nombre, select);
            lblStatus.setText(resultado);

            if (resultado.startsWith("Vista creada")) {
                txtNombre.clear();
                textSelect.clear();
                cargarArbolObjetos();
            }
        });

        section.getChildren().addAll(lblTitulo, hboxNombre, lblSelect, textSelect, btnCrear);
        return section;
    }

    // ================================================================
    // TAB 3: EJECUTOR SQL
    // ================================================================
    private void crearTabSQLExecutor() {
        Tab tabSQL = new Tab();
        tabSQL.setText("Ejecutor SQL");
        tabSQL.setClosable(false);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(16));
        vbox.setStyle("-fx-background-color: #0f1117;");

        Label lblTitulo = new Label("Ejecuta SELECT, INSERT, UPDATE, DELETE:");
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        TextArea textSQL = new TextArea();
        textSQL.setPrefRowCount(6);
        textSQL.setWrapText(true);
        textSQL.setText("SELECT * FROM ");
        aplicarEstiloDarkTextArea(textSQL);
        VBox.setVgrow(textSQL, Priority.ALWAYS);

        Button btnEjecutar = new Button("Ejecutar");
        btnEjecutar.setStyle("-fx-padding: 8px 16px;");
        btnEjecutar.setOnAction(e -> {
            String sql = textSQL.getText().trim();
            if (sql.isEmpty()) {
                lblStatus.setText("ERROR: Ingresa SQL");
                return;
            }
            ejecutarSQL(sql, vbox);
        });

        vbox.getChildren().addAll(lblTitulo, textSQL, btnEjecutar);
        tabSQL.setContent(vbox);
        tabPane.getTabs().add(tabSQL);
    }

    private void ejecutarSQL(String sql, VBox contenedor) {
        new Thread(() -> {
            try {
                if (sql.toLowerCase().startsWith("select")) {
                    List<Map<String, String>> resultados = metadata.ejecutarSelectRetornarMapa(sql);
                    javafx.application.Platform.runLater(() -> {
                        mostrarResultados(resultados, contenedor);
                        lblStatus.setText("SELECT: " + resultados.size() + " filas");
                    });
                } else {
                    int filas = metadata.ejecutarDML(sql);
                    javafx.application.Platform.runLater(() -> {
                        lblStatus.setText("OK: " + filas + " filas afectadas");
                        cargarArbolObjetos();
                    });
                }
            } catch (SQLException ex) {
                javafx.application.Platform.runLater(() ->
                    lblStatus.setText("ERROR: " + ex.getMessage())
                );
            }
        }).start();
    }

    private void mostrarResultados(List<Map<String, String>> resultados, VBox contenedor) {
        if (resultados.isEmpty()) {
            lblStatus.setText("Sin resultados");
            return;
        }

        ObservableList<Node> children = contenedor.getChildren();
        if (children.size() > 3) {
            children.remove(3, children.size());
        }

        TableView<Map<String, String>> tabla = new TableView<>();
        Set<String> columnas = resultados.get(0).keySet();

        for (String colNombre : columnas) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(colNombre);
            col.setCellValueFactory(cellData -> {
                Map<String, String> fila = cellData.getValue();
                String valor = fila.getOrDefault(colNombre, "");
                return new SimpleStringProperty(valor);
            });
            col.setPrefWidth(100);
            tabla.getColumns().add(col);
        }

        ObservableList<Map<String, String>> datos = FXCollections.observableArrayList(resultados);
        tabla.setItems(datos);
        tabla.setPrefHeight(200);
        tabla.setStyle("-fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0;");

        contenedor.getChildren().add(tabla);
    }

    // ================================================================
    // HELPER: Aplicar estilo dark a TextArea
    // ================================================================
    private void aplicarEstiloDarkTextArea(TextArea textArea) {
        textArea.setStyle(
            "-fx-font-family: 'Courier New';" +
            "-fx-font-size: 10px;" +
            "-fx-background-color: #1a1d27;" +
            "-fx-control-inner-background: #1a1d27;" +
            "-fx-text-fill: #e2e8f0;" +
            "-fx-highlight-fill: #4ade80;" +
            "-fx-highlight-text-fill: #0f1117;" +
            "-fx-border-color: #2a2d3a;" +
            "-fx-border-radius: 4;" +
            "-fx-padding: 8;"
        );
    }

    // ================================================================
    // BOTONES PRINCIPALES
    // ================================================================
    @FXML
    public void onRefrescar() {
        lblStatus.setText("Recargando...");
        cargarArbolObjetos();
    }

    @FXML
    public void onDesconectar() {
        ConexionManager.cerrar();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Database Manager Tool");
            stage.setScene(new Scene(root, 520, 600));
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.show();

            Stage mainStage = (Stage) lblStatus.getScene().getWindow();
            mainStage.close();

        } catch (IOException e) {
            lblStatus.setText("Error: " + e.getMessage());
        }
    }
}
