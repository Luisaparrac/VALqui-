package com.valqui.negocio;

import com.valqui.modelo.Cobertura;
import com.valqui.modelo.Vehiculo;

import java.util.List;

/** HU-03 / R15: detalle del vehículo con las condiciones de la compañía. */
public record DetalleVehiculo(
        Vehiculo vehiculo,
        String compania,
        double valorDeposito,
        double franquiciaDanos,
        Integer kilometrajeIncluido,
        boolean kilometrajeIlimitado,
        PoliticaCancelacion politicaCancelacion,
        List<Cobertura> coberturas) {

    public record PoliticaCancelacion(int horasCancelacionGratuita, double penalizacionCancelacion) {
    }
}
