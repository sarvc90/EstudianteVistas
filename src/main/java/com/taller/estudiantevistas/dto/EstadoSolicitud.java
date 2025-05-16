package com.taller.estudiantevistas.dto;

import com.google.gson.annotations.SerializedName;

public enum EstadoSolicitud {
    @SerializedName("PENDIENTE") PENDIENTE,
    @SerializedName("EN_PROCESO") EN_PROCESO,
    @SerializedName("RESUELTA") RESUELTA,
    @SerializedName("CANCELADA") CANCELADA
}