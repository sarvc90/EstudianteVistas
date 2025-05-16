package com.taller.estudiantevistas.dto;

import com.google.gson.annotations.SerializedName;

public enum TipoContenido {
    @SerializedName("DOCUMENTO") DOCUMENTO,
    @SerializedName("VIDEO") VIDEO,
    @SerializedName("ENLACE") ENLACE,
    @SerializedName("PRESENTACION") PRESENTACION,
    @SerializedName("OTRO") OTRO
}