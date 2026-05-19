package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.ConexionGuardada;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;


public class ConexionStorage {

    private static final String DIR_APP  = System.getProperty("user.home") + "/.dbmanager";
    private static final String ARCHIVO  = DIR_APP + "/conexiones.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<ConexionGuardada> cargar() {
        try {
            File archivo = new File(ARCHIVO);
            if (!archivo.exists()) return new ArrayList<>();

            Reader reader = new FileReader(archivo);
            Type tipo = new TypeToken<List<ConexionGuardada>>() {}.getType();
            List<ConexionGuardada> lista = GSON.fromJson(reader, tipo);
            reader.close();
            return lista != null ? lista : new ArrayList<>();

        } catch (IOException e) {
            System.err.println("Error al cargar conexiones: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void guardar(List<ConexionGuardada> conexiones) {
        try {
            Files.createDirectories(Paths.get(DIR_APP));
            Writer writer = new FileWriter(ARCHIVO);
            GSON.toJson(conexiones, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("Error al guardar conexiones: " + e.getMessage());
        }
    }

    public static void agregar(ConexionGuardada nueva) {
        List<ConexionGuardada> lista = cargar();
        lista.add(nueva);
        guardar(lista);
    }

    public static void eliminar(String alias) {
        List<ConexionGuardada> lista = cargar();
        lista.removeIf(c -> c.getAlias().equals(alias));
        guardar(lista);
    }
}
