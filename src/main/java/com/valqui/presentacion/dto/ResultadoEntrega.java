package com.valqui.presentacion.dto;

import com.valqui.modelo.EstadoReserva;
import com.valqui.modelo.EstadoVehiculo;
import com.valqui.modelo.PenalizacionDano;

/**
 * Resultado de una entrega o devolución. EntregaService devuelve void, así que
 * EntregaController lo arma con el código, el resultado de la operación y el daño recibido.
 */
public record ResultadoEntrega(
        String codigo,
        EstadoReserva estadoReserva,
        EstadoVehiculo estadoVehiculo,
        PenalizacionDano penalizacionDano) {
}
