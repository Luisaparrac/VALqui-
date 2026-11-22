package com.valqui.negocio;

import com.valqui.modelo.EstadoReserva;
import com.valqui.modelo.EstadoVehiculo;
import com.valqui.modelo.PenalizacionDano;

/** Resultado de una entrega o devolución: estado de la reserva y del vehículo. */
public record ResultadoEntrega(
        String codigo,
        EstadoReserva estadoReserva,
        String placa,
        EstadoVehiculo estadoVehiculo,
        String sucursalVehiculoId,
        PenalizacionDano penalizacionDano) {
}
