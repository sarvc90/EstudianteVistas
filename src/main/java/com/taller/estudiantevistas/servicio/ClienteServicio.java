package com.taller.estudiantevistas.servicio;

import com.google.gson.*;
import com.taller.estudiantevistas.dto.Estudiante;
import java.io.*;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClienteServicio {
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private Gson gson;

    public ClienteServicio(String host, int puerto) throws IOException {
        this.socket = new Socket(host, puerto);
        this.salida = new PrintWriter(socket.getOutputStream(), true);
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Crear Gson con deserializadores personalizados para fechas
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                    @Override
                    public Date deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        try {
                            if (json.isJsonNull()) {
                                return null;
                            }

                            String dateStr = json.getAsString();
                            if (dateStr == null || dateStr.isEmpty()) {
                                return null;
                            }

                            // Try different date formats
                            SimpleDateFormat[] formats = {
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                                    new SimpleDateFormat("yyyy-MM-dd"),
                                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"),
                                    new SimpleDateFormat("dd/MM/yyyy")
                            };

                            for (SimpleDateFormat format : formats) {
                                try {
                                    return format.parse(dateStr);
                                } catch (ParseException e) {
                                    // Try next format
                                }
                            }

                            System.err.println("⚠️ No se pudo parsear la fecha: " + dateStr);
                            return null;
                        } catch (Exception e) {
                            System.err.println("❌ Error al deserializar fecha: " + e.getMessage());
                            return null;
                        }
                    }
                })
                .registerTypeAdapter(java.sql.Date.class, new JsonDeserializer<java.sql.Date>() {
                    @Override
                    public java.sql.Date deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        Date date = context.deserialize(json, Date.class);
                        return date != null ? new java.sql.Date(date.getTime()) : null;
                    }
                })
                .create();

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

    /**
     * Solicita los contenidos educativos del servidor
     * @param userId ID del usuario para personalizar los contenidos
     * @return Lista de contenidos en formato JSON
     * @throws IOException Si hay problemas de comunicación con el servidor
     * @throws RuntimeException Si la respuesta del servidor es inválida
     */
    public JsonArray obtenerContenidosEducativos(String userId) throws IOException {
        try {
            // 1. Preparar solicitud
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "OBTENER_CONTENIDOS");

            JsonObject datos = new JsonObject();
            datos.addProperty("userId", userId);
            solicitud.add("datos", datos);

            // 2. Enviar solicitud
            salida.println(solicitud.toString());
            salida.flush();

            // 3. Recibir respuesta
            String respuesta = entrada.readLine();
            if (respuesta == null) {
                throw new IOException("El servidor no respondió (respuesta nula)");
            }

            // 4. Parsear respuesta manualmente para evitar problemas con fechas
            JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

            // 5. Validar estructura básica
            if (!jsonRespuesta.has("exito")) {
                throw new RuntimeException("Respuesta mal formada: falta campo 'exito'");
            }

            // 6. Manejar respuesta fallida
            if (!jsonRespuesta.get("exito").getAsBoolean()) {
                String mensajeError = jsonRespuesta.has("mensaje")
                        ? jsonRespuesta.get("mensaje").getAsString()
                        : "Error desconocido del servidor";
                throw new RuntimeException(mensajeError);
            }

            // 7. Validar y retornar contenidos
            if (!jsonRespuesta.has("contenidos")) {
                throw new RuntimeException("Respuesta mal formada: falta campo 'contenidos'");
            }

            return jsonRespuesta.getAsJsonArray("contenidos");
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Respuesta del servidor no es un JSON válido", e);
        } catch (IOException e) {
            System.err.println("[ERROR] Error de comunicación al obtener contenidos: " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            System.err.println("[ERROR] Error al procesar respuesta de contenidos: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Obtiene todos los contenidos educativos disponibles con manejo robusto de fechas
     * @return JsonArray con los contenidos
     * @throws IOException Si hay error de comunicación con el servidor
     */
    public JsonArray obtenerTodosContenidos() throws IOException {
        try {
            // 1. Preparar solicitud
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "OBTENER_CONTENIDOS");
            solicitud.add("datos", new JsonObject());

            // 2. Enviar solicitud
            salida.println(solicitud.toString());
            salida.flush();

            // 3. Recibir respuesta
            String respuestaStr = entrada.readLine();
            if (respuestaStr == null) {
                throw new IOException("El servidor no respondió");
            }

            System.out.println("📥 Respuesta recibida (cruda): " + respuestaStr);

            // 4. Parsear respuesta usando JsonParser directamente
            JsonElement respuestaElement = JsonParser.parseString(respuestaStr);
            if (!respuestaElement.isJsonObject()) {
                throw new IOException("Respuesta del servidor no es un objeto JSON válido");
            }

            JsonObject respuesta = respuestaElement.getAsJsonObject();

            // 5. Verificar éxito
            if (!respuesta.has("exito") || !respuesta.get("exito").getAsBoolean()) {
                String mensajeError = respuesta.has("mensaje")
                        ? respuesta.get("mensaje").getAsString()
                        : "Error desconocido al obtener contenidos";
                throw new IOException(mensajeError);
            }

            // 6. Obtener contenidos directamente sin intentar deserializar las fechas automáticamente
            if (!respuesta.has("contenidos")) {
                throw new IOException("Respuesta mal formada: falta campo 'contenidos'");
            }

            return respuesta.getAsJsonArray("contenidos");

        } catch (JsonSyntaxException e) {
            throw new IOException("Error al parsear respuesta JSON: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Error al obtener contenidos: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene todas las solicitudes de ayuda de la red social
     * @return Lista de todas las solicitudes en formato JSON
     * @throws IOException Si hay problemas de comunicación con el servidor
     */
    public JsonArray obtenerTodasSolicitudes() throws IOException {
        try {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "OBTENER_SOLICITUDES");
            solicitud.add("datos", new JsonObject()); // Datos vacíos

            enviarSolicitud(solicitud);
            JsonObject respuesta = recibirRespuesta();

            if (!respuesta.get("exito").getAsBoolean()) {
                throw new IOException(respuesta.has("mensaje") ?
                        respuesta.get("mensaje").getAsString() : "Error al obtener solicitudes");
            }

            return respuesta.getAsJsonArray("solicitudes");
        } catch (JsonSyntaxException e) {
            throw new IOException("Respuesta del servidor no es un JSON válido", e);
        }
    }

    // Métodos auxiliares reutilizables
    public void enviarSolicitud(JsonObject solicitud) throws IOException {
        salida.println(solicitud.toString());
        salida.flush();
        System.out.println("📤 Solicitud enviada: " + solicitud);
    }

    private JsonObject recibirRespuesta() throws IOException {
        String respuestaStr = entrada.readLine();
        if (respuestaStr == null) {
            throw new IOException("El servidor no respondió");
        }
        System.out.println("📥 Respuesta recibida: " + respuestaStr);
        return JsonParser.parseString(respuestaStr).getAsJsonObject();
    }

    /**
     * Solicita las solicitudes de ayuda del servidor
     * @param userId ID del usuario para personalizar las solicitudes
     * @return Lista de solicitudes en formato JSON
     */
    public JsonArray obtenerSolicitudesAyuda(String userId) throws IOException {
        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "OBTENER_SOLICITUDES");

        JsonObject datos = new JsonObject();
        datos.addProperty("userId", userId);
        solicitud.add("datos", datos);

        salida.println(solicitud.toString());
        String respuesta = entrada.readLine();

        if (respuesta == null) {
            throw new IOException("El servidor no respondió");
        }

        JsonObject jsonRespuesta = JsonParser.parseString(respuesta).getAsJsonObject();

        if (!jsonRespuesta.get("exito").getAsBoolean()) {
            throw new IOException(jsonRespuesta.has("mensaje") ?
                    jsonRespuesta.get("mensaje").getAsString() : "Error desconocido al obtener solicitudes");
        }

        return jsonRespuesta.getAsJsonArray("solicitudes");
    }

    public JsonObject actualizarUsuario(JsonObject datosUsuario) {
        try {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "ACTUALIZAR_USUARIO");
            solicitud.add("datos", datosUsuario);

            salida.println(solicitud.toString());
            String respuesta = entrada.readLine();
            return JsonParser.parseString(respuesta).getAsJsonObject();
        } catch (IOException e) {
            JsonObject error = new JsonObject();
            error.addProperty("exito", false);
            error.addProperty("mensaje", "Error de conexión: " + e.getMessage());
            return error;
        }
    }

    public JsonObject eliminarUsuario(String usuarioId) {
        try {
            JsonObject solicitud = new JsonObject();
            solicitud.addProperty("tipo", "ELIMINAR_USUARIO");
            solicitud.addProperty("usuarioId", usuarioId);

            salida.println(solicitud.toString());
            String respuesta = entrada.readLine();
            return JsonParser.parseString(respuesta).getAsJsonObject();
        } catch (IOException e) {
            JsonObject error = new JsonObject();
            error.addProperty("exito", false);
            error.addProperty("mensaje", "Error de conexión: " + e.getMessage());
            return error;
        }
    }

}