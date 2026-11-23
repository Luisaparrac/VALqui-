package com.valqui.presentacion.dto;

import com.valqui.modelo.Cobertura;
import com.valqui.modelo.CompaniaAliada;
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

    /** Arma el detalle con el vehículo que devuelve VehiculoService.consultarVehiculo. */
    public static DetalleVehiculo de(Vehiculo vehiculo) {
        CompaniaAliada compania = vehiculo.getCompania();
        return new DetalleVehiculo(vehiculo, compania.getNombre(), compania.getValorDeposito(),
                vehiculo.getFranquiciaDanos(), vehiculo.getKilometrajeIncluido(),
                vehiculo.getKilometrajeIncluido() == null,
                new PoliticaCancelacion(compania.getHorasCancelacionGratuita(),
                        compania.getPenalizacionCancelacion()),
                compania.getCoberturas());
    }
}
