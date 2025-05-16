package com.taller.estudiantevistas.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * DTO para transferencia de datos de SolicitudAyuda entre cliente y servidor
 */
public class SolicitudAyuda {
    @Expose
    private String id;

    @Expose
    private String tema;

    @Expose
    private String descripcion;

    @Expose
    private Date fecha;

    @Expose
    private Urgencia urgencia;

    @SerializedName("solicitanteId")
    @Expose
    private String autorId;

    @Expose
    private String autorNombre;

    @Expose
    private EstadoSolicitud estado;

    // Constructor vacío para deserialización
    public SolicitudAyuda() {}

    // Constructor completo
    public SolicitudAyuda(String id, String tema, String descripcion, Date fecha,
                          Urgencia urgencia, String autorId, String autorNombre,
                          EstadoSolicitud estado) {
        this.id = id;
        this.tema = tema;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.urgencia = urgencia;
        this.autorId = autorId;
        this.autorNombre = autorNombre;
        this.estado = estado;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Urgencia getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(Urgencia urgencia) {
        this.urgencia = urgencia;
    }

    public String getAutorId() {
        return autorId;
    }

    public void setAutorId(String autorId) {
        this.autorId = autorId;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public void setAutorNombre(String autorNombre) {
        this.autorNombre = autorNombre;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "SolicitudAyuda{" +
                "id='" + id + '\'' +
                ", tema='" + tema + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", fecha=" + fecha +
                ", urgencia=" + urgencia +
                ", autorId='" + autorId + '\'' +
                ", autorNombre='" + autorNombre + '\'' +
                ", estado=" + estado +
                '}';
    }
}