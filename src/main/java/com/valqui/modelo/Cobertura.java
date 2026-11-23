package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.annotation.Transient;

/** Cobertura embebida en la compañía aliada (composición «ofrece»). Se identifica por su nombre. */
public class Cobertura {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    private double costo;

    /** Valor recibido en el JSON: un double no distingue "no enviado" de 0. No se guarda. */
    @Transient
    @JsonIgnore
    @NotNull(message = "El costo es obligatorio")
    @PositiveOrZero(message = "El costo no puede ser negativo")
    private Double costoRecibido;

    public Cobertura() {
    }

    public Cobertura(String nombre, double costo) {
        this.nombre = nombre;
        this.costo = costo;
    }

    @JsonSetter("costo")
    private void recibirCosto(Double valor) {
        this.costoRecibido = valor;
        this.costo = valor == null ? 0 : valor;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getCosto() { return costo; }
    public void setCosto(double costo) { this.costo = costo; }
}
