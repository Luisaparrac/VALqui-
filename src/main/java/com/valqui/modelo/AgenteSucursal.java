package com.valqui.modelo;

/** Agente que trabaja en una sucursal de una compañía (relación «trabaja en»). */
public class AgenteSucursal extends Usuario {

    private String codigoEmpleado;
    /** «trabaja en»: nombre de la sucursal dentro de la compañía. */
    private String sucursal;
    private String companiaNit;

    public AgenteSucursal() {
        super(AGENTE);
    }

    public void entregarVehiculo(Reserva reserva, boolean documentosValidos) {
        if (!sucursal.equals(reserva.getSucursalRecogida())) {
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

    /** valorDano = 0 significa que el vehículo vuelve sin daño. */
    public void recibirVehiculo(Reserva reserva, double valorDano, String motivoDano) {
        if (!sucursal.equals(reserva.getSucursalDevolucion())) {
            throw new NegocioException(403, "La reserva no se devuelve en la sucursal del agente");
        }
        if (valorDano != 0) {
            reserva.registrarPenalizacion(valorDano, motivoDano);
        }
        reserva.finalizarAlquiler();
    }

    public String getCodigoEmpleado() { return codigoEmpleado; }
    public void setCodigoEmpleado(String codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; }
    public String getSucursal() { return sucursal; }
    public void setSucursal(String sucursal) { this.sucursal = sucursal; }
    public String getCompaniaNit() { return companiaNit; }
    public void setCompaniaNit(String companiaNit) { this.companiaNit = companiaNit; }
}
