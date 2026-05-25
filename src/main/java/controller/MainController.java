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
            cargarBasesDeDatos(conn, raiz);
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
        }
    }

    private void cargarBasesDeDatos(Connection conn, TreeItem<String> raiz) throws SQLException {
        TreeItem<String> nodo = new TreeItem<>("Bases de datos");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                nodo.getChildren().add(new TreeItem<>(rs.getString(1)));
            }
        }
        raiz.getChildren().add(nodo);
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
                nodo.getChildren().add(new TreeItem<>(nombre + " [" + evento + "]"));
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
        textDDL.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0;");
        textDDL.setEditable(false);
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
            default:
                return "Selecciona una tabla, vista, procedimiento o funcion";
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
        vbox.setStyle("-fx-background-color: #0f1117;");

        Label lblTitulo = new Label("Crear nueva tabla:");
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        HBox hboxNombre = new HBox(10);
        Label lblNombre = new Label("Nombre: ");
        lblNombre.setStyle("-fx-text-fill: #e2e8f0; -fx-min-width: 100;");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("usuarios");
        txtNombre.setStyle("-fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0;");
        hboxNombre.getChildren().addAll(lblNombre, txtNombre);

        Label lblColsTitle = new Label("Columnas:");
        lblColsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0;");

        TextArea textColsInput = new TextArea();
        textColsInput.setPrefRowCount(8);
        textColsInput.setWrapText(true);
        textColsInput.setText("id INT AUTO_INCREMENT PRIMARY KEY,\nnombre VARCHAR(100) NOT NULL,\nemail VARCHAR(100) UNIQUE");
        textColsInput.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0;");
        VBox.setVgrow(textColsInput, Priority.ALWAYS);

        Button btnCrear = new Button("Crear Tabla");
        btnCrear.setStyle("-fx-padding: 8px 16px;");
        btnCrear.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                lblStatus.setText("ERROR: Ingresa nombre de tabla");
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

        vbox.getChildren().addAll(lblTitulo, hboxNombre, lblColsTitle, textColsInput, btnCrear);
        tabEditor.setContent(vbox);
        tabPane.getTabs().add(tabEditor);
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
        textSQL.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0;");
        textSQL.setText("SELECT * FROM ");
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
