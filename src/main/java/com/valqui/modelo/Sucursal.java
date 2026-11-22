package com.valqui.modelo;

/** Sucursal embebida en la compañía aliada (composición «tiene»). */
public class Sucursal {

    private String id;
    private String nombre;
    private String ciudad;
    private String direccion;
    private String coordenadas;
    private String horarioAtencion;

    public Sucursal() {
    }

    public Sucursal(String nombre, String ciudad, String direccion, String coordenadas, String horarioAtencion) {
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.coordenadas = coordenadas;
        this.horarioAtencion = horarioAtencion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getCoordenadas() { return coordenadas; }
    public void setCoordenadas(String coordenadas) { this.coordenadas = coordenadas; }
    public String getHorarioAtencion() { return horarioAtencion; }
    public void setHorarioAtencion(String horarioAtencion) { this.horarioAtencion = horarioAtencion; }
}
