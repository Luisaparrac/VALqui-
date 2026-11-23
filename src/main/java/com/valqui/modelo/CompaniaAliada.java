package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "companias")
@JsonIgnoreProperties(value = {"valorDeposito", "horasCancelacionGratuita", "penalizacionCancelacion",
        "sucursales", "coberturas"}, allowGetters = true)
public class CompaniaAliada {

    @Id
    @NotBlank(message = "El NIT es obligatorio")
    private String nit;
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotEmpty(message = "Debe indicar al menos una ciudad de operación")
    private List<@NotBlank String> ciudadesOperacion = new ArrayList<>();
    private double valorDeposito;
    private int horasCancelacionGratuita;
    private double penalizacionCancelacion;
    private List<Sucursal> sucursales = new ArrayList<>();
    private List<Cobertura> coberturas = new ArrayList<>();

    public boolean operaEn(String ciudad) {
        return ciudad != null && ciudadesOperacion != null
                && ciudadesOperacion.stream().anyMatch(c -> c.trim().equalsIgnoreCase(ciudad.trim()));
    }

    public void agregarSucursal(Sucursal sucursal) {
        if (!operaEn(sucursal.getCiudad())) {
            throw new NegocioException(400, "La compañía no opera en esa ciudad");
        }
        if (sucursales.stream().anyMatch(s -> s.getNombre().equalsIgnoreCase(sucursal.getNombre()))) {
            throw new NegocioException(409, "Ya existe una sucursal con ese nombre en la compañía");
        }
        sucursales.add(sucursal);
    }

    public void agregarCobertura(Cobertura cobertura) {
        if (coberturas.stream().anyMatch(c -> c.getNombre().equalsIgnoreCase(cobertura.getNombre()))) {
            throw new NegocioException(409, "Ya existe una cobertura con ese nombre en la compañía");
        }
        coberturas.add(cobertura);
    }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<String> getCiudadesOperacion() { return ciudadesOperacion; }
    public void setCiudadesOperacion(List<String> ciudadesOperacion) { this.ciudadesOperacion = ciudadesOperacion; }
    public double getValorDeposito() { return valorDeposito; }
    public void setValorDeposito(double valorDeposito) { this.valorDeposito = valorDeposito; }
    public int getHorasCancelacionGratuita() { return horasCancelacionGratuita; }
    public void setHorasCancelacionGratuita(int horasCancelacionGratuita) { this.horasCancelacionGratuita = horasCancelacionGratuita; }
    public double getPenalizacionCancelacion() { return penalizacionCancelacion; }
    public void setPenalizacionCancelacion(double penalizacionCancelacion) { this.penalizacionCancelacion = penalizacionCancelacion; }
    public List<Sucursal> getSucursales() { return sucursales; }
    public List<Cobertura> getCoberturas() { return coberturas; }
}
