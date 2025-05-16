package com.taller.estudiantevistas.dto;

import java.util.Arrays;
import java.util.List;

public class Estudiante {
    private String nombres;
    private String apellidos;
    private String cedula;
    private String correo;
    private String contrasena;
    private List<String> intereses;

    public Estudiante(String nombres, String apellidos, String cedula,
                      String correo, String contrasena, String intereses) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.cedula = cedula;
        this.correo = correo;
        this.contrasena = contrasena;
        this.intereses = Arrays.asList(intereses.split(","));
    }

    // Getters y Setters
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCedula() { return cedula; }
    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
    public List<String> getIntereses() { return intereses; }

    public enum EstadoSolicitud {
        PENDIENTE,
        EN_PROCESO,
        RESUELTA,
        CANCELADA
    }
}