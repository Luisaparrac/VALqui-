package com.valqui.modelo;

import com.valqui.negocio.NegocioException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "companias")
public class CompaniaAliada {

    @Id
    private String id;
    @Indexed(unique = true)
    private String nit;
    private String nombre;
    private List<String> ciudadesOperacion = new ArrayList<>();
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
        if (sucursal.getId() == null) {
            sucursal.setId(UUID.randomUUID().toString());
        }
        sucursales.add(sucursal);
    }

    public void agregarCobertura(Cobertura cobertura) {
        if (cobertura.getId() == null) {
            cobertura.setId(UUID.randomUUID().toString());
        }
        coberturas.add(cobertura);
    }

    public String getId() { return id; }
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
