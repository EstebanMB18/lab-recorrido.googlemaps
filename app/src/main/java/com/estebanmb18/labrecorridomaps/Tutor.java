package com.estebanmb18.labrecorridomaps;

public class Tutor {
    private final String nombre;
    private final String materia;
    private final double latitud;
    private final double longitud;

    public Tutor(String nombre, String materia, double latitud, double longitud) {
        this.nombre = nombre;
        this.materia = materia;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNombre() { return nombre; }
    public String getMateria() { return materia; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}
