package com.taller.estudiantevistas.dto;

import java.util.Arrays;
import java.util.List;

public class Estudiante {
    private String nombres;
    private String correo;
    private String contrasena;
    private List<String> intereses;

    public Estudiante(String nombres,
                      String correo, String contrasena, String intereses) {
        this.nombres = nombres;
        this.correo = correo;
        this.contrasena = contrasena;
        this.intereses = Arrays.asList(intereses.split(","));
    }

    // Getters y Setters
    public String getNombres() { return nombres; }
    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
    public List<String> getIntereses() { return intereses; }

}