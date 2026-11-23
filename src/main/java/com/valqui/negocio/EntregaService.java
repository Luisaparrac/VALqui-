package com.valqui.negocio;

import com.valqui.modelo.EstadoVehiculo;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Reserva;
import org.springframework.stereotype.Service;

@Service
public class EntregaService {

    private final ReservaService reservaService;
    private final VehiculoService vehiculoService;

    public EntregaService(ReservaService reservaService, VehiculoService vehiculoService) {
        this.reservaService = reservaService;
        this.vehiculoService = vehiculoService;
    }

    /** HU-16: con documentos válidos, la reserva pasa a EN_CURSO y el vehículo a RESERVADO. */
    public void entregar(String codigo, boolean documentosValidos) {
        if (!documentosValidos) {
            throw new NegocioException(400, "Documentos no válidos, no se puede iniciar el alquiler");
        }
        Reserva reserva = reservaService.iniciarAlquiler(codigo);
        vehiculoService.cambiarEstado(reserva.getVehiculoPlaca(), EstadoVehiculo.RESERVADO);
    }

    /** HU-17: la reserva pasa a FINALIZADA y el vehículo vuelve a DISPONIBLE en la sucursal de devolución. */
    public void recibir(String codigo, double valorDano, String motivoDano) {
        Reserva reserva = reservaService.finalizarAlquiler(codigo, valorDano, motivoDano);
        vehiculoService.registrarDevolucion(reserva.getVehiculoPlaca(), reserva.getSucursalDevolucion());
    }
}
