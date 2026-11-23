package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "vehiculos")
@JsonIgnoreProperties(value = {"estado", "companiaNit", "sucursalCiudad"}, allowGetters = true)
public class Vehiculo {

    @Id
    @NotBlank(message = "La placa es obligatoria")
    private String placa;
    @NotBlank(message = "La marca es obligatoria")
    private String marca;
    @NotBlank(message = "El modelo es obligatorio")
    private String modelo;
    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;
    private int anio;
    @NotBlank(message = "La transmisión es obligatoria")
    private String transmision;
    private int capacidadPasajeros;
    private double tarifaDiaria;
    /** null = kilometraje ilimitado. */
    @Positive(message = "El kilometraje incluido debe ser mayor que 0 (vacío = ilimitado)")
    private Integer kilometrajeIncluido;
    private double franquiciaDanos;
    private EstadoVehiculo estado;
    /** «es dueña de» */
    private String companiaNit;
    /** «tiene asignados»: nombre de la sucursal actual dentro de la compañía. */
    @NotBlank(message = "La sucursal es obligatoria")
    private String sucursal;
    private String sucursalCiudad;

    /** Reservas activas del vehículo; las carga VehiculoService. */
    @Transient
    @JsonIgnore
    private List<Reserva> reservasActivas = new ArrayList<>();

    /** Compañía dueña («es dueña de»); la carga VehiculoService.consultarVehiculo. */
    @Transient
    @JsonIgnore
    private CompaniaAliada compania;

    // Valores recibidos en el JSON: un primitivo no distingue "no enviado" de 0. No se guardan.
    @Transient
    @JsonIgnore
    @NotNull(message = "El año es obligatorio")
    @Min(value = 1900, message = "El año no es válido")
    private Integer anioRecibido;
    @Transient
    @JsonIgnore
    @NotNull(message = "La capacidad de pasajeros es obligatoria")
    @Positive(message = "La capacidad de pasajeros debe ser mayor que 0")
    private Integer capacidadRecibida;
    @Transient
    @JsonIgnore
    @NotNull(message = "La tarifa diaria es obligatoria")
    @Positive(message = "La tarifa diaria debe ser mayor que 0")
    private Double tarifaRecibida;
    @Transient
    @JsonIgnore
    @NotNull(message = "La franquicia de daños es obligatoria")
    @PositiveOrZero(message = "La franquicia no puede ser negativa")
    private Double franquiciaRecibida;

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
        this.sucursal = sucursal.getNombre();
        this.sucursalCiudad = sucursal.getCiudad();
    }

    @JsonSetter("anio")
    private void recibirAnio(Integer valor) {
        this.anioRecibido = valor;
        this.anio = valor == null ? 0 : valor;
    }

    @JsonSetter("capacidadPasajeros")
    private void recibirCapacidad(Integer valor) {
        this.capacidadRecibida = valor;
        this.capacidadPasajeros = valor == null ? 0 : valor;
    }

    @JsonSetter("tarifaDiaria")
    private void recibirTarifa(Double valor) {
        this.tarifaRecibida = valor;
        this.tarifaDiaria = valor == null ? 0 : valor;
    }

    @JsonSetter("franquiciaDanos")
    private void recibirFranquicia(Double valor) {
        this.franquiciaRecibida = valor;
        this.franquiciaDanos = valor == null ? 0 : valor;
    }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public int getAnio() { return anio; }
    public String getTransmision() { return transmision; }
    public void setTransmision(String transmision) { this.transmision = transmision; }
    public int getCapacidadPasajeros() { return capacidadPasajeros; }
    public double getTarifaDiaria() { return tarifaDiaria; }
    public Integer getKilometrajeIncluido() { return kilometrajeIncluido; }
    public void setKilometrajeIncluido(Integer kilometrajeIncluido) { this.kilometrajeIncluido = kilometrajeIncluido; }
    public double getFranquiciaDanos() { return franquiciaDanos; }
    public EstadoVehiculo getEstado() { return estado; }
    public String getCompaniaNit() { return companiaNit; }
    public void setCompaniaNit(String companiaNit) { this.companiaNit = companiaNit; }
    public String getSucursal() { return sucursal; }
    public void setSucursal(String sucursal) { this.sucursal = sucursal; }
    public String getSucursalCiudad() { return sucursalCiudad; }
    public List<Reserva> getReservasActivas() { return reservasActivas; }
    public void setReservasActivas(List<Reserva> reservasActivas) { this.reservasActivas = reservasActivas; }
    public CompaniaAliada getCompania() { return compania; }
    public void setCompania(CompaniaAliada compania) { this.compania = compania; }
}
