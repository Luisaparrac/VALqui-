package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(value = {"id", "tipo", "metodosPago"}, allowGetters = true)
public class Cliente extends Usuario {

    @NotNull(message = "El campo fecha de nacimiento es obligatorio")
    @Past(message = "La fecha de nacimiento debe ser pasada")
    private LocalDate fechaNacimiento;
    @NotBlank(message = "El número de contacto es obligatorio")
    private String numeroContacto;
    @NotBlank(message = "El país de residencia es obligatorio")
    private String paisResidencia;
    /** Composición: los métodos de pago viven embebidos en el cliente. */
    private List<MetodoPago> metodosPago = new ArrayList<>();

    public Cliente() {
        super(CLIENTE);
    }

    public boolean esMayorDeEdad() {
        return fechaNacimiento != null
                && Period.between(fechaNacimiento, LocalDate.now()).getYears() >= 18;
    }

    public void agregarMetodoPago(MetodoPago metodoPago) {
        if (metodosPago == null) {
            metodosPago = new ArrayList<>();
        }
        metodosPago.add(metodoPago);
    }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getNumeroContacto() { return numeroContacto; }
    public void setNumeroContacto(String numeroContacto) { this.numeroContacto = numeroContacto; }
    public String getPaisResidencia() { return paisResidencia; }
    public void setPaisResidencia(String paisResidencia) { this.paisResidencia = paisResidencia; }
    public List<MetodoPago> getMetodosPago() { return metodosPago; }
}
