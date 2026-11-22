package com.valqui.modelo;

import com.valqui.negocio.NegocioException;

/** Agente que trabaja en una sucursal de una compañía (relación «trabaja en»). */
public class AgenteSucursal extends Usuario {

    private String codigoEmpleado;
    private String sucursalId;
    private String companiaNit;

    public AgenteSucursal() {
        super(AGENTE);
    }

    public void entregarVehiculo(Reserva reserva, boolean documentosValidos) {
        if (!sucursalId.equals(reserva.getSucursalRecogidaId())) {
            throw new NegocioException(403, "La reserva no se recoge en la sucursal del agente");
        }
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new NegocioException(409, "La reserva no está confirmada");
        }
        if (!documentosValidos) {
            throw new NegocioException(400, "Documentos no válidos, no se puede iniciar el alquiler");
        }
        reserva.iniciarAlquiler();
    }

    public void recibirVehiculo(Reserva reserva, Double valorDano, String motivoDano) {
        if (!sucursalId.equals(reserva.getSucursalDevolucionId())) {
            throw new NegocioException(403, "La reserva no se devuelve en la sucursal del agente");
        }
        if (valorDano != null) {
            reserva.registrarPenalizacion(valorDano, motivoDano);
        }
        reserva.finalizarAlquiler();
    }

    public String getCodigoEmpleado() { return codigoEmpleado; }
    public void setCodigoEmpleado(String codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; }
    public String getSucursalId() { return sucursalId; }
    public void setSucursalId(String sucursalId) { this.sucursalId = sucursalId; }
    public String getCompaniaNit() { return companiaNit; }
    public void setCompaniaNit(String companiaNit) { this.companiaNit = companiaNit; }
}
