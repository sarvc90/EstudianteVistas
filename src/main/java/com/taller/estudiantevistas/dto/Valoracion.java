package com.taller.estudiantevistas.dto;

import com.google.gson.annotations.Expose;
import java.util.Date;

/**
 * DTO para valoraciones de contenido
 */
public class Valoracion {
    @Expose
    private String id;

    @Expose
    private String autor;

    @Expose
    private int puntuacion;

    @Expose
    private String comentario;

    @Expose
    private Date fecha;

    // Constructor vacío para deserialización
    public Valoracion() {}

    // Constructor completo
    public Valoracion(String id, String autor, int puntuacion, String comentario, Date fecha) {
        this.id = id;
        this.autor = autor;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    @Override
    public String toString() {
        return "Valoracion{" +
                "id='" + id + '\'' +
                ", autor='" + autor + '\'' +
                ", puntuacion=" + puntuacion +
                ", comentario='" + comentario + '\'' +
                ", fecha=" + fecha +
                '}';
    }
}