package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Del JSON de entrada solo se aceptan la placa, las fechas y las sucursales. */
@Document(collection = "reservas")
@JsonIgnoreProperties({"codigo", "estado", "tarifaDiaria", "kilometrajeIncluido", "franquiciaDanos", "valorDeposito",
        "clienteId", "coberturaNombres", "metodoPago", "penalizacionDano", "coberturas",
        "horasCancelacionGratuita", "penalizacionCancelacion", "penalizacionAplicada"})
public class Reserva {

    @Id
    private String codigo;
    @NotNull(message = "La fecha y hora de recogida es obligatoria")
    private LocalDateTime fechaHoraRecogida;
    @NotNull(message = "La fecha y hora de devolución es obligatoria")
    private LocalDateTime fechaHoraDevolucion;
    private EstadoReserva estado;

    // Copias tomadas del vehículo al crear la reserva
    private double tarifaDiaria;
    private Integer kilometrajeIncluido;
    private double franquiciaDanos;
    // Copia tomada de la compañía al crear la reserva
    private double valorDeposito;

    /** «realiza» */
    private String clienteId;
    /** «reserva» */
    @JsonProperty("placa")
    @NotBlank(message = "La placa es obligatoria")
    private String vehiculoPlaca;
    /** «recogida en» / «devolución en»: nombre de la sucursal dentro de la compañía. */
    @NotBlank(message = "La sucursal de recogida es obligatoria")
    private String sucursalRecogida;
    @NotBlank(message = "La sucursal de devolución es obligatoria")
    private String sucursalDevolucion;
    /** «incluye» (0..*): nombres de las coberturas. */
    private List<String> coberturaNombres = new ArrayList<>();
    /** «se paga con» (0..1): últimos dígitos del método de pago usado */
    private String metodoPago;
    /** «genera» (composición, 0..1) */
    private PenalizacionDano penalizacionDano;

    /** Coberturas incluidas (con su costo), las carga ReservaService para calcular el total. */
    @Transient
    private List<Cobertura> coberturas = new ArrayList<>();

    // Política vigente de la compañía: ReservaService la entrega justo antes de cancelar. No se guarda.
    @Transient
    private int horasCancelacionGratuita;
    @Transient
    private double penalizacionCancelacion;
    /** Resultado de la última cancelación, para la respuesta. No se guarda. */
    @Transient
    private double penalizacionAplicada;

    /** La reserva nace en PENDIENTE. */
    public Reserva() {
        this.estado = EstadoReserva.PENDIENTE;
    }

    public double calcularTotal() {
        long horas = Duration.between(fechaHoraRecogida, fechaHoraDevolucion).toHours();
        long dias = Math.max(1, (long) Math.ceil(horas / 24.0));
        double costoCoberturas = coberturas.stream().mapToDouble(Cobertura::getCosto).sum();
        return dias * tarifaDiaria + costoCoberturas;
    }

    public void agregarCobertura(Cobertura cobertura) {
        if (estado != EstadoReserva.PENDIENTE) {
            throw new NegocioException(409, "Solo se pueden agregar coberturas a una reserva pendiente");
        }
        if (coberturaNombres.contains(cobertura.getNombre())) {
            throw new NegocioException(409, "La cobertura ya está incluida en la reserva");
        }
        coberturaNombres.add(cobertura.getNombre());
        coberturas = new ArrayList<>(coberturas);
        coberturas.add(cobertura);
    }

    public void confirmar(MetodoPago metodoPago) {
        if (estado != EstadoReserva.PENDIENTE) {
            throw new NegocioException(409, "Solo se puede confirmar una reserva pendiente");
        }
        this.metodoPago = metodoPago.getUltimosDigitos();
        this.estado = EstadoReserva.CONFIRMADA;
    }

    /**
     * Cancela la reserva y devuelve la penalización aplicada.
     * PENDIENTE: siempre sin costo. CONFIRMADA: sin costo si faltan al menos
     * horasCancelacionGratuita para la recogida; si no, penalizacionCancelacion.
     */
    public double cancelar(LocalDateTime fecha) {
        double penalizacion;
        if (estado == EstadoReserva.PENDIENTE) {
            penalizacion = 0;
        } else if (estado == EstadoReserva.CONFIRMADA) {
            boolean aTiempo = !fecha.plusHours(horasCancelacionGratuita).isAfter(fechaHoraRecogida);
            penalizacion = aTiempo ? 0 : penalizacionCancelacion;
        } else {
            String nombreEstado = estado.name().toLowerCase().replace('_', ' ');
            throw new NegocioException(409, "No es posible cancelar una reserva en estado " + nombreEstado);
        }
        this.estado = EstadoReserva.CANCELADA;
        this.penalizacionAplicada = penalizacion;
        return penalizacion;
    }

    public void iniciarAlquiler() {
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new NegocioException(409, "La reserva no está confirmada");
        }
        this.estado = EstadoReserva.EN_CURSO;
    }

    public void finalizarAlquiler() {
        if (estado != EstadoReserva.EN_CURSO) {
            throw new NegocioException(409, "La reserva no está en curso");
        }
        this.estado = EstadoReserva.FINALIZADA;
    }

    public void registrarPenalizacion(double valor, String motivo) {
        if (penalizacionDano != null) {
            throw new NegocioException(409, "La reserva ya tiene una penalización por daño registrada");
        }
        if (valor <= 0 || motivo == null || motivo.isBlank()) {
            throw new NegocioException(400, "El valor del daño debe ser mayor que 0 y el motivo es obligatorio");
        }
        this.penalizacionDano = new PenalizacionDano(valor, motivo);
    }

    /** Única definición de "reserva activa". */
    public boolean estaActiva() {
        return estado == EstadoReserva.PENDIENTE
                || estado == EstadoReserva.CONFIRMADA
                || estado == EstadoReserva.EN_CURSO;
    }

    public boolean seCruzaCon(LocalDateTime desde, LocalDateTime hasta) {
        return estaActiva() && fechaHoraRecogida.isBefore(hasta) && fechaHoraDevolucion.isAfter(desde);
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public LocalDateTime getFechaHoraRecogida() { return fechaHoraRecogida; }
    public void setFechaHoraRecogida(LocalDateTime fechaHoraRecogida) { this.fechaHoraRecogida = fechaHoraRecogida; }
    public LocalDateTime getFechaHoraDevolucion() { return fechaHoraDevolucion; }
    public void setFechaHoraDevolucion(LocalDateTime fechaHoraDevolucion) { this.fechaHoraDevolucion = fechaHoraDevolucion; }
    public EstadoReserva getEstado() { return estado; }
    public double getTarifaDiaria() { return tarifaDiaria; }
    public void setTarifaDiaria(double tarifaDiaria) { this.tarifaDiaria = tarifaDiaria; }
    public Integer getKilometrajeIncluido() { return kilometrajeIncluido; }
    public void setKilometrajeIncluido(Integer kilometrajeIncluido) { this.kilometrajeIncluido = kilometrajeIncluido; }
    public double getFranquiciaDanos() { return franquiciaDanos; }
    public void setFranquiciaDanos(double franquiciaDanos) { this.franquiciaDanos = franquiciaDanos; }
    public double getValorDeposito() { return valorDeposito; }
    public void setValorDeposito(double valorDeposito) { this.valorDeposito = valorDeposito; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getVehiculoPlaca() { return vehiculoPlaca; }
    public void setVehiculoPlaca(String vehiculoPlaca) { this.vehiculoPlaca = vehiculoPlaca; }
    public String getSucursalRecogida() { return sucursalRecogida; }
    public void setSucursalRecogida(String sucursalRecogida) { this.sucursalRecogida = sucursalRecogida; }
    public String getSucursalDevolucion() { return sucursalDevolucion; }
    public void setSucursalDevolucion(String sucursalDevolucion) { this.sucursalDevolucion = sucursalDevolucion; }
    public List<String> getCoberturaNombres() { return coberturaNombres; }
    public String getMetodoPago() { return metodoPago; }
    public PenalizacionDano getPenalizacionDano() { return penalizacionDano; }
    public List<Cobertura> getCoberturas() { return coberturas; }
    public void setCoberturas(List<Cobertura> coberturas) { this.coberturas = coberturas; }
    public void setHorasCancelacionGratuita(int horasCancelacionGratuita) { this.horasCancelacionGratuita = horasCancelacionGratuita; }
    public void setPenalizacionCancelacion(double penalizacionCancelacion) { this.penalizacionCancelacion = penalizacionCancelacion; }
    public double getPenalizacionAplicada() { return penalizacionAplicada; }
}
