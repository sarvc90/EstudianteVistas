package com.taller.estudiantevistas.dto;

import com.google.gson.annotations.SerializedName;

public enum TipoContenido {
    @SerializedName("IMAGEN") IMAGEN,
    @SerializedName("DOCUMENTO") DOCUMENTO,
    @SerializedName("VIDEO") VIDEO,
    @SerializedName("ENLACE") ENLACE,
    @SerializedName("PRESENTACION") PRESENTACION,
    @SerializedName("OTRO") OTRO;
    public static TipoContenido determinarPorExtension(String nombreArchivo) {
        if (nombreArchivo == null) return OTRO;

        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            case "jpg": case "jpeg": case "png": case "gif": case "bmp":
                return IMAGEN;
            case "mp4": case "avi": case "mov": case "mkv":
                return VIDEO;
            case "pdf": case "doc": case "docx": case "txt":
                return DOCUMENTO;
            case "http": case "https": case "www":
                return ENLACE;
            default:
                return OTRO;
        }
    }
}