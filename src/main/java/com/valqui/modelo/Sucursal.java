package com.valqui.modelo;

import jakarta.validation.constraints.NotBlank;

/** Sucursal embebida en la compañía aliada (composición «tiene»). Se identifica por su nombre. */
public class Sucursal {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotBlank(message = "El campo ubicación es obligatorio")
    private String ciudad;
    @NotBlank(message = "El campo ubicación es obligatorio")
    private String direccion;
    private String coordenadas;
    @NotBlank(message = "El horario de atención es obligatorio")
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
