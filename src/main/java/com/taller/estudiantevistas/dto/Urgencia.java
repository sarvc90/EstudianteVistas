package com.taller.estudiantevistas.dto;

import com.google.gson.annotations.SerializedName;

public enum Urgencia {
    @SerializedName("ALTA") ALTA,
    @SerializedName("MEDIA") MEDIA,
    @SerializedName("BAJA") BAJA
}