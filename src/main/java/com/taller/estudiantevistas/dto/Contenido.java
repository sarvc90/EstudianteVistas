package com.taller.estudiantevistas.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para transferencia de datos de Contenido educativo entre cliente y servidor
 */
public class Contenido {
    @Expose
    private String id;

    @Expose
    private String titulo;

    @Expose
    private String autor;

    @SerializedName("fechaCreacion")
    @Expose
    private LocalDateTime fechaPublicacion;

    @Expose
    private TipoContenido tipo;

    @Expose
    private String tema;

    @Expose
    private String descripcion;

    @Expose
    private String contenido;

    @Expose
    private List<Valoracion> valoraciones;

    @Expose
    private double promedioValoraciones;

    // Constructor vacío para deserialización
    public Contenido() {}

    // Constructor completo
    public Contenido(String id, String titulo, String autor, LocalDateTime fechaPublicacion,
                     TipoContenido tipo, String tema, String descripcion, String contenido,
                     List<Valoracion> valoraciones, double promedioValoraciones) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.fechaPublicacion = fechaPublicacion;
        this.tipo = tipo;
        this.tema = tema;
        this.descripcion = descripcion;
        this.contenido = contenido;
        this.valoraciones = valoraciones;
        this.promedioValoraciones = promedioValoraciones;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public TipoContenido getTipo() {
        return tipo;
    }

    public void setTipo(TipoContenido tipo) {
        this.tipo = tipo;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public List<Valoracion> getValoraciones() {
        return valoraciones;
    }

    public void setValoraciones(List<Valoracion> valoraciones) {
        this.valoraciones = valoraciones;
    }

    public double getPromedioValoraciones() {
        return promedioValoraciones;
    }

    public void setPromedioValoraciones(double promedioValoraciones) {
        this.promedioValoraciones = promedioValoraciones;
    }

    @Override

    public String toString() {
        return "Contenido{" +
                "id='" + id + '\'' +
                ", titulo='" + titulo + '\'' +
                ", autor='" + autor + '\'' +
                ", fechaPublicacion=" + fechaPublicacion +
                ", tipo=" + tipo +
                ", tema='" + tema + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", contenido='" + contenido + '\'' +  // Incluido en toString
                ", valoraciones=" + valoraciones +
                ", promedioValoraciones=" + promedioValoraciones +
                '}';
    }
}