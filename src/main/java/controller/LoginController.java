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
import java.util.List;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private ListView<ConexionGuardada> listConexiones;
    @FXML private TextField txtAlias;
    @FXML private TextField txtHost;
    @FXML private TextField txtPuerto;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private TextField txtBaseDatos;
    @FXML private Label lblMensaje;

    private ObservableList<ConexionGuardada> conexiones;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("[LOGIN] Inicializando LoginController...");
        
        conexiones = FXCollections.observableArrayList(ConexionStorage.cargar());
        listConexiones.setItems(conexiones);

        listConexiones.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, nueva) -> {
                if (nueva != null) {
                    System.out.println("[LOGIN] Conexion seleccionada: " + nueva.getAlias());
                    cargarEnFormulario(nueva);
                }
            }
        );

        txtHost.setText("localhost");
        txtPuerto.setText("3306");

        if (!conexiones.isEmpty()) {
            listConexiones.getSelectionModel().selectFirst();
        }
        
        System.out.println("[LOGIN] LoginController inicializado");
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
        String alias = txtAlias.getText().trim();
        String host = txtHost.getText().trim();
        String puertoStr = txtPuerto.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText();
        String baseDatos = txtBaseDatos.getText().trim();

        if (alias.isBlank()) alias = usuario + "@" + host;
        if (host.isBlank()) host = "localhost";

        int puerto = 3306;
        try {
            puerto = Integer.parseInt(puertoStr);
        } catch (NumberFormatException e) {
            mostrarError("Puerto invalido. Se usara 3306.");
        }

        return new ConexionGuardada(alias, host, puerto, usuario, contrasena, baseDatos);
    }

    @FXML
    private void onNuevaConexion() {
        System.out.println("[LOGIN] Nueva conexion");
        txtAlias.clear();
        txtHost.setText("localhost");
        txtPuerto.setText("3306");
        txtUsuario.clear();
        txtContrasena.clear();
        txtBaseDatos.clear();
        listConexiones.getSelectionModel().clearSelection();
        limpiarMensaje();
        txtAlias.requestFocus();
    }

    @FXML
    private void onGuardarConexion() {
        System.out.println("[LOGIN] Guardar conexion");
        ConexionGuardada nueva = leerFormulario();

        if (nueva.getUsuario().isBlank()) {
            mostrarError("El usuario es obligatorio.");
            return;
        }

        boolean existe = conexiones.stream()
            .anyMatch(c -> c.getAlias().equals(nueva.getAlias()));

        if (existe) {
            conexiones.replaceAll(c -> c.getAlias().equals(nueva.getAlias()) ? nueva : c);
        } else {
            conexiones.add(nueva);
        }

        ConexionStorage.guardar(conexiones.stream().toList());
        listConexiones.setItems(FXCollections.observableArrayList(conexiones));
        mostrarExito("Conexion guardada: " + nueva.getAlias());
    }

    @FXML
    private void onEliminarConexion() {
        System.out.println("[LOGIN] Eliminar conexion");
        ConexionGuardada seleccionada = listConexiones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarError("Selecciona una conexion para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
            "Eliminar la conexion \"" + seleccionada.getAlias() + "\"?",
            ButtonType.YES, ButtonType.NO);
        confirmacion.setTitle("Eliminar conexion");
        confirmacion.setHeaderText(null);

        confirmacion.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                conexiones.remove(seleccionada);
                ConexionStorage.guardar(conexiones.stream().toList());
                onNuevaConexion();
            }
        });
    }

    @FXML
    private void onProbarConexion() {
        System.out.println("[LOGIN] Probar conexion");
        ConexionGuardada datos = leerFormulario();
        mostrarInfo("Probando conexion...");

        new Thread(() -> {
            String error = ConexionManager.probarConexion(datos);
            javafx.application.Platform.runLater(() -> {
                if (error == null) {
                    mostrarExito("Conexion exitosa con " + datos.getHost());
                } else {
                    mostrarError("Error: " + error);
                }
            });
        }).start();
    }

    @FXML
    private void onConectar() {
        System.out.println("[LOGIN] Conectar - Abriendo ventana principal");
        ConexionGuardada datos = leerFormulario();

        if (datos.getUsuario().isBlank()) {
            mostrarError("Ingresa un usuario.");
            return;
        }

        mostrarInfo("Conectando...");

        new Thread(() -> {
            System.out.println("[LOGIN] Thread: Intentando conectar con: " + datos.getAlias());
            boolean ok = ConexionManager.conectar(datos);
            
            javafx.application.Platform.runLater(() -> {
                if (ok) {
                    System.out.println("[LOGIN] Conexion exitosa, abriendo ventana principal");
                    abrirVentanaPrincipal();
                } else {
                    System.out.println("[LOGIN] Fallo la conexion");
                    mostrarError("No se pudo conectar. Verifica los datos.");
                }
            });
        }).start();
    }

    private void abrirVentanaPrincipal() {
        try {
            System.out.println("[LOGIN] Cargando MainView.fxml...");
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            System.out.println("[LOGIN] MainView.fxml cargado correctamente");

            Stage stage = new Stage();
            stage.setTitle("Database Manager - " + ConexionManager.getDatosActivos().getAlias());
            stage.setScene(new Scene(root, 1200, 800));
            stage.getScene().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());
            stage.show();
            System.out.println("[LOGIN] Ventana principal abierta");

            // Cerrar ventana de login
            Stage loginStage = (Stage) txtHost.getScene().getWindow();
            loginStage.close();
            System.out.println("[LOGIN] Ventana de login cerrada");

        } catch (IOException e) {
            System.err.println("[LOGIN] ERROR al abrir ventana principal: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[LOGIN] ERROR general: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error general: " + e.getMessage());
        }
    }

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
