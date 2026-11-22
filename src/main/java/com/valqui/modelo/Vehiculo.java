package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "vehiculos")
public class Vehiculo {

    @Id
    private String id;
    @Indexed(unique = true)
    private String placa;
    private String marca;
    private String modelo;
    private String categoria;
    private int anio;
    private String transmision;
    private int capacidadPasajeros;
    private double tarifaDiaria;
    /** null = kilometraje ilimitado. */
    private Integer kilometrajeIncluido;
    private double franquiciaDanos;
    private EstadoVehiculo estado;
    /** «es dueña de» */
    private String companiaNit;
    /** Sucursal asignada actual («tiene asignados»). */
    private String sucursalId;
    private String sucursalCiudad;

    /** Reservas activas del vehículo; las carga VehiculoService desde ReservaRepository. */
    @Transient
    @JsonIgnore
    private List<Reserva> reservasActivas = new ArrayList<>();

    public boolean estaDisponible(LocalDateTime desde, LocalDateTime hasta) {
        boolean estadoPermiteReserva = estado == EstadoVehiculo.DISPONIBLE || estado == EstadoVehiculo.RESERVADO;
        return estadoPermiteReserva && reservasActivas.stream().noneMatch(r -> r.seCruzaCon(desde, hasta));
    }

    public boolean tieneReservaActiva() {
        return reservasActivas.stream().anyMatch(Reserva::estaActiva);
    }

    public void cambiarEstado(EstadoVehiculo estado) {
        this.estado = estado;
    }

    public void reasignar(Sucursal sucursal) {
        this.sucursalId = sucursal.getId();
        this.sucursalCiudad = sucursal.getCiudad();
    }

    public String getId() { return id; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
    public String getTransmision() { return transmision; }
    public void setTransmision(String transmision) { this.transmision = transmision; }
    public int getCapacidadPasajeros() { return capacidadPasajeros; }
    public void setCapacidadPasajeros(int capacidadPasajeros) { this.capacidadPasajeros = capacidadPasajeros; }
    public double getTarifaDiaria() { return tarifaDiaria; }
    public void setTarifaDiaria(double tarifaDiaria) { this.tarifaDiaria = tarifaDiaria; }
    public Integer getKilometrajeIncluido() { return kilometrajeIncluido; }
    public void setKilometrajeIncluido(Integer kilometrajeIncluido) { this.kilometrajeIncluido = kilometrajeIncluido; }
    public double getFranquiciaDanos() { return franquiciaDanos; }
    public void setFranquiciaDanos(double franquiciaDanos) { this.franquiciaDanos = franquiciaDanos; }
    public EstadoVehiculo getEstado() { return estado; }
    public String getCompaniaNit() { return companiaNit; }
    public void setCompaniaNit(String companiaNit) { this.companiaNit = companiaNit; }
    public String getSucursalId() { return sucursalId; }
    public String getSucursalCiudad() { return sucursalCiudad; }
    public List<Reserva> getReservasActivas() { return reservasActivas; }
    public void setReservasActivas(List<Reserva> reservasActivas) { this.reservasActivas = reservasActivas; }
}
