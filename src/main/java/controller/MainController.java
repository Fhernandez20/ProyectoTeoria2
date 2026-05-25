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
import javafx.scene.layout.*;
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

    // Objeto seleccionado en el árbol
    private String objetoSeleccionado = "";
    private String tipoObjeto         = "";
    private String tablaDelObjeto     = ""; // para índices: tabla padre

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("[INIT] Iniciando MainController...");

        ConexionGuardada datos = ConexionManager.getDatosActivos();
        if (datos != null) {
            lblConexionActiva.setText(datos.getUsuario() + "@" + datos.getHost()
                + ":" + datos.getPuerto());
            String bd = datos.getBaseDatos();
            lblEstadoBD.setText("BD: " + (bd != null && !bd.isBlank() ? bd : "(ninguna)"));
        }

        Connection conn = ConexionManager.getConexion();
        if (conn == null) {
            lblStatus.setText("ERROR: No hay conexion activa");
            return;
        }

        metadata = new MetadataExtractor(conn);

        crearTabDDL();
        crearTabEditorVisual();
        crearTabSQLExecutor();
        crearTabInfoObjetos();

        cargarArbolObjetos();

        System.out.println("[INIT] MainController iniciado correctamente");
    }

    // =====================================================================
    // ÁRBOL DE OBJETOS
    // =====================================================================

    private void cargarArbolObjetos() {
        Connection conn = ConexionManager.getConexion();
        if (conn == null) return;

        TreeItem<String> raiz = new TreeItem<>("Servidor");
        raiz.setExpanded(true);

        try {
            cargarTablas(raiz);
            cargarVistas(raiz);
            cargarProcedimientos(raiz);
            cargarFunciones(raiz);
            cargarTriggers(raiz);
            cargarIndices(raiz);
            cargarSecuencias(raiz);
            cargarUsuarios(raiz);
            cargarTablespaces(raiz);
            cargarNoAplica(raiz);
        } catch (Exception e) {
            System.err.println("[ARBOL] Error: " + e.getMessage());
            e.printStackTrace();
        }

        treeObjetos.setRoot(raiz);
        configurarSeleccionArbol();
        lblStatus.setText("Objetos cargados");
    }

    private void configurarSeleccionArbol() {
        treeObjetos.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, nueva) -> {
                if (nueva == null) return;
                String texto = nueva.getValue();
                TreeItem<String> padre = nueva.getParent();

                // Nodos de categoría (sin padre o con padre = Servidor)
                if (padre == null || padre.getValue().equals("Servidor")) return;

                // Guardar contexto del objeto seleccionado
                objetoSeleccionado = texto;
                tablaDelObjeto     = "";

                String categoria = padre.getValue();
                switch (categoria) {
                    case "Tablas"        -> tipoObjeto = "TABLA";
                    case "Vistas"        -> tipoObjeto = "VISTA";
                    case "Procedimientos"-> tipoObjeto = "PROCEDIMIENTO";
                    case "Funciones"     -> tipoObjeto = "FUNCION";
                    case "Triggers"      -> tipoObjeto = "TRIGGER";
                    case "Secuencias"    -> tipoObjeto = "SECUENCIA";
                    case "Usuarios"      -> tipoObjeto = "USUARIO";
                    case "Tablespaces"   -> tipoObjeto = "TABLESPACE";
                    default -> {
                        // Índice: padre es el nombre de la tabla
                        if (padre.getParent() != null
                            && "Indices".equals(padre.getParent().getValue())) {
                            tipoObjeto     = "INDICE";
                            tablaDelObjeto = padre.getValue();
                        } else {
                            tipoObjeto = "";
                        }
                    }
                }
            }
        );
    }

    private void cargarTablas(TreeItem<String> raiz) throws SQLException {
        TreeItem<String> nodo = new TreeItem<>("Tablas");
        for (String t : metadata.listarTablas())
            nodo.getChildren().add(new TreeItem<>(t));
        raiz.getChildren().add(nodo);
    }

    private void cargarVistas(TreeItem<String> raiz) throws SQLException {
        TreeItem<String> nodo = new TreeItem<>("Vistas");
        for (String v : metadata.listarVistas())
            nodo.getChildren().add(new TreeItem<>(v));
        raiz.getChildren().add(nodo);
    }

    private void cargarProcedimientos(TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Procedimientos");
        for (String p : metadata.listarProcedimientos())
            nodo.getChildren().add(new TreeItem<>(p));
        raiz.getChildren().add(nodo);
    }

    private void cargarFunciones(TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Funciones");
        for (String f : metadata.listarFunciones())
            nodo.getChildren().add(new TreeItem<>(f));
        raiz.getChildren().add(nodo);
    }

    private void cargarTriggers(TreeItem<String> raiz) throws SQLException {
        TreeItem<String> nodo = new TreeItem<>("Triggers");
        for (Map<String, String> t : metadata.listarTriggers())
            nodo.getChildren().add(new TreeItem<>(t.get("nombre")));
        raiz.getChildren().add(nodo);
    }

    /**
     * Índices: agrupados por tabla dentro del nodo "Indices"
     */
    private void cargarIndices(TreeItem<String> raiz) {
        TreeItem<String> nodoRaiz = new TreeItem<>("Indices");

        List<Map<String, String>> todos = metadata.listarTodosLosIndices();
        // Agrupar por tabla
        Map<String, List<Map<String, String>>> porTabla = new LinkedHashMap<>();
        for (Map<String, String> idx : todos) {
            porTabla.computeIfAbsent(idx.get("tabla"), k -> new ArrayList<>()).add(idx);
        }

        for (Map.Entry<String, List<Map<String, String>>> entry : porTabla.entrySet()) {
            TreeItem<String> nodoTabla = new TreeItem<>(entry.getKey());
            for (Map<String, String> idx : entry.getValue()) {
                String nombre = idx.get("nombre");
                String unico  = idx.get("unique").isEmpty() ? "" : " [UNIQUE]";
                nodoTabla.getChildren().add(new TreeItem<>(nombre + unico));
            }
            nodoRaiz.getChildren().add(nodoTabla);
        }

        raiz.getChildren().add(nodoRaiz);
    }

    /**
     * Secuencias: disponibles en MariaDB 10.3+
     */
    private void cargarSecuencias(TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Secuencias");
        List<String> seqs = metadata.listarSecuencias();
        if (seqs.isEmpty()) {
            nodo.getChildren().add(new TreeItem<>("(ninguna — requiere MariaDB 10.3+)"));
        } else {
            for (String s : seqs)
                nodo.getChildren().add(new TreeItem<>(s));
        }
        raiz.getChildren().add(nodo);
    }

    private void cargarUsuarios(TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Usuarios");
        for (Map<String, String> u : metadata.listarUsuarios())
            nodo.getChildren().add(
                new TreeItem<>(u.get("usuario") + "@" + u.get("host")));
        raiz.getChildren().add(nodo);
    }

    /**
     * Tablespaces: en MariaDB se muestra info de SHOW TABLE STATUS (engine/storage)
     */
    private void cargarTablespaces(TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Tablespaces");
        Set<String> engines = new LinkedHashSet<>();
        for (Map<String, String> ts : metadata.listarTablespaces())
            engines.add(ts.get("engine") != null ? ts.get("engine") : "UNKNOWN");
        for (String engine : engines)
            nodo.getChildren().add(new TreeItem<>(engine));
        if (engines.isEmpty())
            nodo.getChildren().add(new TreeItem<>("(sin tablas)"));
        raiz.getChildren().add(nodo);
    }

    /**
     * Objetos que NO aplican en MariaDB — justificados en la documentación
     */
    private void cargarNoAplica(TreeItem<String> raiz) {
        TreeItem<String> nodo = new TreeItem<>("Paquetes (N/A)");
        nodo.getChildren().add(new TreeItem<>("No aplica en MariaDB"));
        nodo.getChildren().add(new TreeItem<>("(exclusivo de Oracle PL/SQL)"));
        raiz.getChildren().add(nodo);
    }

    // =====================================================================
    // TAB 1: DDL
    // =====================================================================

    private void crearTabDDL() {
        Tab tab = new Tab("DDL");
        tab.setClosable(false);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(16));
        vbox.setStyle("-fx-background-color: #0f1117;");

        Label lbl = new Label("Selecciona un objeto del árbol y presiona 'Cargar DDL'");
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        TextArea textDDL = new TextArea();
        textDDL.setWrapText(false);
        textDDL.setEditable(false);
        aplicarEstiloDark(textDDL);
        VBox.setVgrow(textDDL, Priority.ALWAYS);

        HBox hbox = new HBox(10);
        Button btnCargar = boton("Cargar DDL", "#4ade80", "#0f1117");
        Button btnCopiar = boton("Copiar", "#22d3ee", "#0f1117");

        btnCargar.setOnAction(e -> {
            String ddl = obtenerDDL();
            textDDL.setText(ddl);
            lblStatus.setText("DDL: " + objetoSeleccionado);
        });

        btnCopiar.setOnAction(e -> {
            javafx.scene.input.Clipboard cb =
                javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content =
                new javafx.scene.input.ClipboardContent();
            content.putString(textDDL.getText());
            cb.setContent(content);
            lblStatus.setText("DDL copiado al portapapeles");
        });

        hbox.getChildren().addAll(btnCargar, btnCopiar);
        vbox.getChildren().addAll(lbl, textDDL, hbox);
        tab.setContent(vbox);
        tabPane.getTabs().add(tab);
    }

    private String obtenerDDL() {
        return switch (tipoObjeto) {
            case "TABLA"        -> metadata.getDDLTabla(objetoSeleccionado);
            case "VISTA"        -> metadata.getDDLVista(objetoSeleccionado);
            case "PROCEDIMIENTO"-> metadata.getDDLProcedimiento(objetoSeleccionado);
            case "FUNCION"      -> metadata.getDDLFuncion(objetoSeleccionado);
            case "TRIGGER"      -> metadata.getDDLTrigger(objetoSeleccionado);
            case "SECUENCIA"    -> metadata.getDDLSecuencia(objetoSeleccionado);
            case "INDICE"       -> {
                // Quitar el sufijo " [UNIQUE]" si existe
                String nombre = objetoSeleccionado.replace(" [UNIQUE]", "").trim();
                yield metadata.getDDLIndice(nombre, tablaDelObjeto);
            }
            case "USUARIO"      -> {
                // formato "user@host"
                String[] partes = objetoSeleccionado.split("@", 2);
                String u = partes[0];
                String h = partes.length > 1 ? partes[1] : "%";
                yield metadata.getDDLUsuario(u, h)
                    + "\n\n-- GRANTS:\n"
                    + metadata.getGrantsUsuario(u, h);
            }
            case "TABLESPACE"   -> "-- TABLESPACE: " + objetoSeleccionado + "\n"
                + "-- En MariaDB los tablespaces son gestionados por el storage engine.\n"
                + "-- Este engine está en uso por las tablas de la base de datos.\n"
                + "-- No existe un DDL independiente de tablespace en MariaDB sin\n"
                + "-- usar information_schema.INNODB_TABLESPACES (no permitido).";
            default             -> "-- Selecciona un objeto del árbol primero.";
        };
    }

    // =====================================================================
    // TAB 2: EDITOR VISUAL
    // =====================================================================

    private void crearTabEditorVisual() {
        Tab tab = new Tab("Editor Visual");
        tab.setClosable(false);

        ScrollPane scroll = new ScrollPane();
        scroll.setStyle("-fx-background-color: #0f1117; -fx-border-color: transparent;");
        scroll.setFitToWidth(true);

        VBox main = new VBox(20);
        main.setPadding(new Insets(20));
        main.setStyle("-fx-background-color: #0f1117;");

        main.getChildren().addAll(
            seccionCrearTabla(),
            seccionCrearVista(),
            seccionCrearIndice()
        );

        scroll.setContent(main);
        tab.setContent(scroll);
        tabPane.getTabs().add(tab);
    }

    private VBox seccionCrearTabla() {
        VBox sec = seccion("#4ade80", "#0d1b0f", "📋  Crear Nueva Tabla");

        HBox hNombre = new HBox(10);
        hNombre.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lNombre = label("Nombre tabla:", "#94a3b8", 120);
        TextField txtNombre = campo("ej: productos");
        hNombre.getChildren().addAll(lNombre, txtNombre);

        TextArea txtCols = codeArea("#4ade80",
            "id INT AUTO_INCREMENT PRIMARY KEY,\n"
          + "nombre VARCHAR(100) NOT NULL,\n"
          + "precio DECIMAL(10,2) DEFAULT 0.00");

        Button btn = boton("✓ Crear Tabla", "#4ade80", "#0f1117");
        btn.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) { lblStatus.setText("❌ Ingresa el nombre"); return; }
            String limpio = txtCols.getText().replaceAll("\\s+", " ").trim();
            List<String> cols = Arrays.stream(limpio.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (cols.isEmpty()) { lblStatus.setText("❌ Ingresa columnas"); return; }
            String res = metadata.crearTabla(nombre, cols);
            lblStatus.setText(res);
            if (res.startsWith("Tabla")) { txtNombre.clear(); txtCols.clear(); cargarArbolObjetos(); }
        });

        sec.getChildren().addAll(hNombre, label("Columnas:", "#94a3b8", -1), txtCols, btn);
        return sec;
    }

    private VBox seccionCrearVista() {
        VBox sec = seccion("#22d3ee", "#0a1929", "👁  Crear Nueva Vista");

        HBox hNombre = new HBox(10);
        hNombre.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lNombre = label("Nombre vista:", "#94a3b8", 120);
        TextField txtNombre = campo("ej: vista_activos");
        hNombre.getChildren().addAll(lNombre, txtNombre);

        TextArea txtSelect = codeArea("#22d3ee",
            "SELECT id, nombre FROM productos WHERE activo = 1");

        Button btn = boton("✓ Crear Vista", "#22d3ee", "#0f1117");
        btn.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) { lblStatus.setText("❌ Ingresa el nombre"); return; }
            String sel = txtSelect.getText().trim();
            if (sel.isEmpty()) { lblStatus.setText("❌ Ingresa el SELECT"); return; }
            String res = metadata.crearVista(nombre, sel);
            lblStatus.setText(res);
            if (res.startsWith("Vista")) { txtNombre.clear(); txtSelect.clear(); cargarArbolObjetos(); }
        });

        sec.getChildren().addAll(hNombre, label("Consulta SELECT:", "#94a3b8", -1), txtSelect, btn);
        return sec;
    }

    private VBox seccionCrearIndice() {
        VBox sec = seccion("#f59e0b", "#1a1200", "🗂  Crear Nuevo Índice");

        HBox h1 = new HBox(10); h1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField txtTabla  = campo("nombre_tabla");
        h1.getChildren().addAll(label("Tabla:", "#94a3b8", 120), txtTabla);

        HBox h2 = new HBox(10); h2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField txtIndice = campo("nombre_indice");
        h2.getChildren().addAll(label("Nombre índice:", "#94a3b8", 120), txtIndice);

        HBox h3 = new HBox(10); h3.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField txtCols   = campo("col1, col2");
        h3.getChildren().addAll(label("Columnas:", "#94a3b8", 120), txtCols);

        Button btn = boton("✓ Crear Índice", "#f59e0b", "#0f1117");
        btn.setOnAction(e -> {
            String tabla  = txtTabla.getText().trim();
            String nombre = txtIndice.getText().trim();
            String cols   = txtCols.getText().trim();
            if (tabla.isEmpty() || nombre.isEmpty() || cols.isEmpty()) {
                lblStatus.setText("❌ Completa todos los campos"); return;
            }
            String res = metadata.crearIndice(tabla, nombre, cols);
            lblStatus.setText(res);
            if (res.startsWith("Índice")) cargarArbolObjetos();
        });

        sec.getChildren().addAll(h1, h2, h3, btn);
        return sec;
    }

    // =====================================================================
    // TAB 3: EJECUTOR SQL
    // =====================================================================

    private void crearTabSQLExecutor() {
        Tab tab = new Tab("Ejecutor SQL");
        tab.setClosable(false);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(16));
        vbox.setStyle("-fx-background-color: #0f1117;");

        Label lbl = new Label("Ejecuta SELECT, INSERT, UPDATE, DELETE, DDL:");
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        TextArea textSQL = new TextArea("SELECT * FROM ");
        textSQL.setPrefRowCount(7);
        textSQL.setWrapText(true);
        aplicarEstiloDark(textSQL);
        VBox.setVgrow(textSQL, Priority.ALWAYS);

        HBox hbox = new HBox(10);
        Button btnEjec   = boton("▶ Ejecutar", "#4ade80", "#0f1117");
        Button btnLimpiar = boton("✕ Limpiar", "#f87171", "#0f1117");

        btnEjec.setOnAction(e -> {
            String sql = textSQL.getText().trim();
            if (sql.isEmpty()) { lblStatus.setText("❌ Ingresa SQL"); return; }
            ejecutarSQL(sql, vbox);
        });
        btnLimpiar.setOnAction(e -> {
            textSQL.clear();
            // Eliminar tabla de resultados si existe
            if (vbox.getChildren().size() > 3)
                vbox.getChildren().remove(3, vbox.getChildren().size());
        });

        hbox.getChildren().addAll(btnEjec, btnLimpiar);
        vbox.getChildren().addAll(lbl, textSQL, hbox);
        tab.setContent(vbox);
        tabPane.getTabs().add(tab);
    }

    private void ejecutarSQL(String sql, VBox contenedor) {
        new Thread(() -> {
            try {
                String sqlLower = sql.toLowerCase().trim();
                if (sqlLower.startsWith("select") || sqlLower.startsWith("show")
                    || sqlLower.startsWith("describe") || sqlLower.startsWith("explain")) {
                    List<Map<String, String>> rows = metadata.ejecutarSelectRetornarMapa(sql);
                    javafx.application.Platform.runLater(() -> {
                        mostrarTablaResultados(rows, contenedor);
                        lblStatus.setText("✓ " + rows.size() + " fila(s) retornadas");
                    });
                } else {
                    int afectadas = metadata.ejecutarDML(sql);
                    javafx.application.Platform.runLater(() -> {
                        lblStatus.setText("✓ " + afectadas + " fila(s) afectadas");
                        cargarArbolObjetos();
                    });
                }
            } catch (SQLException ex) {
                javafx.application.Platform.runLater(() ->
                    lblStatus.setText("❌ " + ex.getMessage()));
            }
        }).start();
    }

    private void mostrarTablaResultados(List<Map<String, String>> rows, VBox contenedor) {
        // Quitar resultado anterior
        if (contenedor.getChildren().size() > 3)
            contenedor.getChildren().remove(3, contenedor.getChildren().size());

        if (rows.isEmpty()) { lblStatus.setText("Sin resultados"); return; }

        TableView<Map<String, String>> tabla = new TableView<>();
        for (String col : rows.get(0).keySet()) {
            TableColumn<Map<String, String>, String> tc = new TableColumn<>(col);
            tc.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getOrDefault(col, "")));
            tc.setPrefWidth(120);
            tabla.getColumns().add(tc);
        }
        tabla.setItems(FXCollections.observableArrayList(rows));
        tabla.setPrefHeight(250);
        tabla.setStyle("-fx-background-color: #1a1d27;");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        contenedor.getChildren().add(tabla);
    }

    // =====================================================================
    // TAB 4: INFO DE OBJETOS (tablespaces, usuarios con grants, etc.)
    // =====================================================================

    private void crearTabInfoObjetos() {
        Tab tab = new Tab("Info Objetos");
        tab.setClosable(false);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(16));
        vbox.setStyle("-fx-background-color: #0f1117;");

        Label lbl = new Label("Información detallada del objeto seleccionado en el árbol:");
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        // Selector de tipo de info
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(
            "Columnas de tabla",
            "Índices de tabla",
            "Grants de usuario",
            "Tablespaces (SHOW TABLE STATUS)",
            "Estado de la conexión"
        );
        combo.setValue("Columnas de tabla");
        combo.setStyle("-fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0;");

        TextArea textInfo = new TextArea();
        textInfo.setWrapText(false);
        textInfo.setEditable(false);
        aplicarEstiloDark(textInfo);
        VBox.setVgrow(textInfo, Priority.ALWAYS);

        Button btn = boton("Obtener Info", "#22d3ee", "#0f1117");
        btn.setOnAction(e -> {
            String seleccion = combo.getValue();
            StringBuilder sb = new StringBuilder();

            switch (seleccion) {
                case "Columnas de tabla" -> {
                    if (!"TABLA".equals(tipoObjeto)) {
                        sb.append("Selecciona una TABLA en el árbol primero.");
                    } else {
                        sb.append("-- Columnas de: ").append(objetoSeleccionado).append("\n\n");
                        metadata.getColumnasTabla(objetoSeleccionado)
                            .forEach((k, v) -> sb.append(String.format("  %-25s %s%n", k, v)));
                    }
                }
                case "Índices de tabla" -> {
                    String tabla = "TABLA".equals(tipoObjeto) ? objetoSeleccionado : tablaDelObjeto;
                    if (tabla.isBlank()) {
                        sb.append("Selecciona una TABLA o ÍNDICE en el árbol primero.");
                    } else {
                        sb.append("-- Índices de: ").append(tabla).append("\n\n");
                        for (Map<String, String> idx : metadata.listarIndicesDeTabla(tabla)) {
                            sb.append(String.format("  %-20s %-8s %-10s %s%n",
                                idx.get("nombre"), idx.get("unique"),
                                idx.get("tipo"), idx.get("columna")));
                        }
                    }
                }
                case "Grants de usuario" -> {
                    if (!"USUARIO".equals(tipoObjeto)) {
                        sb.append("Selecciona un USUARIO en el árbol primero.");
                    } else {
                        String[] p = objetoSeleccionado.split("@", 2);
                        sb.append(metadata.getGrantsUsuario(p[0], p.length > 1 ? p[1] : "%"));
                    }
                }
                case "Tablespaces (SHOW TABLE STATUS)" -> {
                    sb.append("-- Información de storage engines (equivalente a tablespaces en MariaDB)\n");
                    sb.append(String.format("%-30s %-10s %-10s %-15s %s%n",
                        "TABLA", "ENGINE", "FILAS", "DATA_LENGTH", "ROW_FORMAT"));
                    sb.append("-".repeat(80)).append("\n");
                    for (Map<String, String> ts : metadata.listarTablespaces()) {
                        sb.append(String.format("%-30s %-10s %-10s %-15s %s%n",
                            ts.get("tabla"), ts.get("engine"),
                            ts.get("rows"), ts.get("data_length"),
                            ts.get("row_format")));
                    }
                }
                case "Estado de la conexión" -> {
                    try {
                        Connection conn = ConexionManager.getConexion();
                        DatabaseMetaData meta = conn.getMetaData();
                        sb.append("-- Estado de la conexión activa\n\n");
                        sb.append("Producto:    ").append(meta.getDatabaseProductName()).append("\n");
                        sb.append("Versión:     ").append(meta.getDatabaseProductVersion()).append("\n");
                        sb.append("Driver:      ").append(meta.getDriverName()).append("\n");
                        sb.append("URL:         ").append(meta.getURL()).append("\n");
                        sb.append("Usuario:     ").append(meta.getUserName()).append("\n");
                        sb.append("BD activa:   ").append(conn.getCatalog()).append("\n");
                        sb.append("AutoCommit:  ").append(conn.getAutoCommit()).append("\n");
                        sb.append("ReadOnly:    ").append(conn.isReadOnly()).append("\n");
                    } catch (SQLException ex) {
                        sb.append("Error: ").append(ex.getMessage());
                    }
                }
            }

            textInfo.setText(sb.toString());
            lblStatus.setText("Info cargada: " + seleccion);
        });

        vbox.getChildren().addAll(lbl, combo, textInfo, btn);
        tab.setContent(vbox);
        tabPane.getTabs().add(tab);
    }

    // =====================================================================
    // BOTONES PRINCIPALES (FXML)
    // =====================================================================

    @FXML
    public void onRefrescar() {
        lblStatus.setText("Recargando...");
        new Thread(() -> javafx.application.Platform.runLater(this::cargarArbolObjetos)).start();
    }

    @FXML
    public void onDesconectar() {
        ConexionManager.cerrar();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Database Manager Tool");
            stage.setScene(new Scene(root, 520, 700));
            stage.getScene().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());
            stage.show();
            ((Stage) lblStatus.getScene().getWindow()).close();
        } catch (IOException e) {
            lblStatus.setText("Error: " + e.getMessage());
        }
    }

    // =====================================================================
    // HELPERS DE UI
    // =====================================================================

    private void aplicarEstiloDark(TextArea ta) {
        ta.setStyle(
            "-fx-font-family: 'Courier New';" +
            "-fx-font-size: 11px;" +
            "-fx-background-color: #1a1d27;" +
            "-fx-control-inner-background: #1a1d27;" +
            "-fx-text-fill: #e2e8f0;" +
            "-fx-border-color: #2a2d3a;" +
            "-fx-border-radius: 4;" +
            "-fx-padding: 8;"
        );
    }

    private Button boton(String texto, String bg, String fg) {
        Button b = new Button(texto);
        b.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 8 16 8 16;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 4;"
        );
        return b;
    }

    private Label label(String texto, String color, double minWidth) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: " + color + ";" +
                   (minWidth > 0 ? "-fx-min-width: " + minWidth + ";" : ""));
        return l;
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #1a1d27; -fx-text-fill: #e2e8f0; -fx-padding: 8;");
        tf.setPrefWidth(300);
        HBox.setHgrow(tf, Priority.ALWAYS);
        return tf;
    }

    private TextArea codeArea(String color, String placeholder) {
        TextArea ta = new TextArea(placeholder);
        ta.setPrefRowCount(4);
        ta.setWrapText(true);
        ta.setStyle(
            "-fx-font-family: 'Courier New';" +
            "-fx-font-size: 11px;" +
            "-fx-background-color: #1a1d27;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-control-inner-background: #1a1d27;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-width: 1;" +
            "-fx-padding: 8;"
        );
        VBox.setVgrow(ta, Priority.SOMETIMES);
        return ta;
    }

    private VBox seccion(String borderColor, String bgColor, String titulo) {
        VBox sec = new VBox(10);
        sec.setStyle(
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 2;" +
            "-fx-padding: 15;" +
            "-fx-border-radius: 6;" +
            "-fx-background-color: " + bgColor + ";"
        );
        Label lbl = new Label(titulo);
        lbl.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + borderColor + ";"
        );
        sec.getChildren().add(lbl);
        return sec;
    }
}
