package com.taller.estudiantevistas.controlador;

import com.google.gson.JsonArray;

public interface ActualizacionListener {
    void onContenidosActualizados(JsonArray contenidos);
    void onSolicitudesActualizadas(JsonArray solicitudes);
}