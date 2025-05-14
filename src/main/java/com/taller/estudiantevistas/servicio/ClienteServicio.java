package com.taller.estudiantevistas.servicio;

import com.google.gson.*;
import com.taller.estudiantevistas.dto.Estudiante;
import java.io.*;
import java.net.Socket;

public class ClienteServicio {
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private Gson gson;

    public ClienteServicio(String host, int puerto) throws IOException {
        this.socket = new Socket(host, puerto);
        this.salida = new PrintWriter(socket.getOutputStream(), true);
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.gson = new GsonBuilder().create();
        System.out.println("🔗 Conectado al servidor en " + host + ":" + puerto);
    }

    public boolean registrarEstudiante(Estudiante estudiante) {
        try {
            JsonObject mensaje = new JsonObject();
            mensaje.addProperty("tipo", "REGISTRO"); // ✅ Corregido para coincidir con el servidor
            mensaje.add("datos", gson.toJsonTree(estudiante));

            System.out.println("📤 JSON enviado al servidor: " + mensaje.toString());
            salida.println(mensaje.toString());
            salida.flush(); // ✅ Asegurar que los datos se envíen completamente

            String respuesta = entrada.readLine();
            if (respuesta == null) {
                System.err.println("❌ Error: Respuesta nula del servidor.");
                return false;
            }

            JsonObject jsonRespuesta = gson.fromJson(respuesta, JsonObject.class);
            System.out.println("📥 Respuesta del servidor: " + jsonRespuesta);

            return jsonRespuesta.get("exito").getAsBoolean();
        } catch (IOException e) {
            System.err.println("❌ Error al comunicarse con el servidor: " + e.getMessage());
            return false;
        }
    }

    public void cerrarConexion() {
        try {
            if (socket != null) socket.close();
            if (salida != null) salida.close();
            if (entrada != null) entrada.close();
            System.out.println("🔌 Conexión cerrada con el servidor.");
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }

    public PrintWriter getSalida() {
        return salida;
    }

    public BufferedReader getEntrada() {
        return entrada;
    }

    public boolean estaConectado() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}