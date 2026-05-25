package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.ConexionGuardada;
import service.ConexionManager;
import service.ConexionStorage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private ListView<ConexionGuardada> listConexiones;
    @FXML private TextField     txtAlias;
    @FXML private TextField     txtHost;
    @FXML private TextField     txtPuerto;
    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private TextField     txtBaseDatos;
    @FXML private Label         lblMensaje;

    private ObservableList<ConexionGuardada> conexiones;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conexiones = FXCollections.observableArrayList(ConexionStorage.cargar());
        listConexiones.setItems(conexiones);

        listConexiones.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, nueva) -> { if (nueva != null) cargarEnFormulario(nueva); }
        );

        txtHost.setText("localhost");
        txtPuerto.setText("3306");

        if (!conexiones.isEmpty())
            listConexiones.getSelectionModel().selectFirst();
    }

    private void cargarEnFormulario(ConexionGuardada c) {
        txtAlias.setText(c.getAlias());
        txtHost.setText(c.getHost());
        txtPuerto.setText(String.valueOf(c.getPuerto()));
        txtUsuario.setText(c.getUsuario());
        txtContrasena.setText(c.getContrasena());
        txtBaseDatos.setText(c.getBaseDatos() != null ? c.getBaseDatos() : "");
        limpiarMensaje();
    }

    private ConexionGuardada leerFormulario() {
        String alias     = txtAlias.getText().trim();
        String host      = txtHost.getText().trim();
        String puertoStr = txtPuerto.getText().trim();
        String usuario   = txtUsuario.getText().trim();
        String contra    = txtContrasena.getText();
        String bd        = txtBaseDatos.getText().trim();

        if (alias.isBlank())  alias = usuario + "@" + host;
        if (host.isBlank())   host  = "localhost";

        int puerto = 3306;
        try { puerto = Integer.parseInt(puertoStr); }
        catch (NumberFormatException e) { mostrarError("Puerto inválido, se usará 3306."); }

        return new ConexionGuardada(alias, host, puerto, usuario, contra, bd);
    }

    @FXML private void onNuevaConexion() {
        txtAlias.clear(); txtHost.setText("localhost"); txtPuerto.setText("3306");
        txtUsuario.clear(); txtContrasena.clear(); txtBaseDatos.clear();
        listConexiones.getSelectionModel().clearSelection();
        limpiarMensaje();
        txtAlias.requestFocus();
    }

    @FXML private void onGuardarConexion() {
        ConexionGuardada nueva = leerFormulario();
        if (nueva.getUsuario().isBlank()) { mostrarError("El usuario es obligatorio."); return; }

        boolean existe = conexiones.stream().anyMatch(c -> c.getAlias().equals(nueva.getAlias()));
        if (existe) conexiones.replaceAll(c -> c.getAlias().equals(nueva.getAlias()) ? nueva : c);
        else        conexiones.add(nueva);

        ConexionStorage.guardar(conexiones.stream().toList());
        listConexiones.setItems(FXCollections.observableArrayList(conexiones));
        mostrarExito("Conexión guardada: " + nueva.getAlias());
    }

    @FXML private void onEliminarConexion() {
        ConexionGuardada sel = listConexiones.getSelectionModel().getSelectedItem();
        if (sel == null) { mostrarError("Selecciona una conexión para eliminar."); return; }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar \"" + sel.getAlias() + "\"?", ButtonType.YES, ButtonType.NO);
        alerta.setTitle("Eliminar conexión"); alerta.setHeaderText(null);
        alerta.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                conexiones.remove(sel);
                ConexionStorage.guardar(conexiones.stream().toList());
                onNuevaConexion();
            }
        });
    }

    @FXML private void onProbarConexion() {
        ConexionGuardada datos = leerFormulario();
        mostrarInfo("Probando conexión...");
        new Thread(() -> {
            String error = ConexionManager.probarConexion(datos);
            javafx.application.Platform.runLater(() -> {
                if (error == null) mostrarExito("✓ Conexión exitosa con " + datos.getHost());
                else               mostrarError("❌ " + error);
            });
        }).start();
    }

    @FXML private void onConectar() {
        ConexionGuardada datos = leerFormulario();
        if (datos.getUsuario().isBlank()) { mostrarError("Ingresa un usuario."); return; }

        mostrarInfo("Conectando...");
        new Thread(() -> {
            boolean ok = ConexionManager.conectar(datos);
            javafx.application.Platform.runLater(() -> {
                if (ok) abrirVentanaPrincipal();
                else    mostrarError("❌ No se pudo conectar. Verifica los datos.");
            });
        }).start();
    }

    private void abrirVentanaPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Database Manager — " + ConexionManager.getDatosActivos().getAlias());

            Scene scene = new Scene(root, 1280, 860);
            // Cargar ambos CSS en la ventana principal también
            scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());
            scene.getStylesheets().add(
                getClass().getResource("/css/tabs.css").toExternalForm());

            stage.setScene(scene);
            stage.show();

            ((Stage) txtHost.getScene().getWindow()).close();

        } catch (IOException e) {
            mostrarError("Error al abrir ventana principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de mensajes
    // -------------------------------------------------------------------------
    private void mostrarError(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.getStyleClass().removeAll("msg-exito", "msg-info");
        if (!lblMensaje.getStyleClass().contains("msg-error"))
            lblMensaje.getStyleClass().add("msg-error");
    }
    private void mostrarExito(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.getStyleClass().removeAll("msg-error", "msg-info");
        if (!lblMensaje.getStyleClass().contains("msg-exito"))
            lblMensaje.getStyleClass().add("msg-exito");
    }
    private void mostrarInfo(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.getStyleClass().removeAll("msg-error", "msg-exito");
        if (!lblMensaje.getStyleClass().contains("msg-info"))
            lblMensaje.getStyleClass().add("msg-info");
    }
    private void limpiarMensaje() {
        lblMensaje.setText("");
        lblMensaje.getStyleClass().removeAll("msg-error", "msg-exito", "msg-info");
    }
}
