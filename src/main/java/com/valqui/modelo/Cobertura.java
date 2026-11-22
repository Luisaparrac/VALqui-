package com.valqui.modelo;

/** Cobertura embebida en la compañía aliada (composición «ofrece»). */
public class Cobertura {

    private String id;
    private String nombre;
    private double costo;

    public Cobertura() {
    }

    public Cobertura(String nombre, double costo) {
        this.nombre = nombre;
        this.costo = costo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getCosto() { return costo; }
    public void setCosto(double costo) { this.costo = costo; }
}
