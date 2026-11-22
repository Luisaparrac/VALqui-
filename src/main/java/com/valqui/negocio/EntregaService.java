package com.valqui.negocio;

import com.valqui.modelo.AgenteSucursal;
import com.valqui.modelo.Reserva;
import com.valqui.modelo.Vehiculo;
import org.springframework.stereotype.Service;

@Service
public class EntregaService {

    private final ReservaService reservaService;
    private final VehiculoService vehiculoService;

    public EntregaService(ReservaService reservaService, VehiculoService vehiculoService) {
        this.reservaService = reservaService;
        this.vehiculoService = vehiculoService;
    }

    /** HU-16: la reserva pasa a EN_CURSO y el vehículo a RESERVADO. */
    public ResultadoEntrega entregar(AgenteSucursal agente, String codigo, boolean documentosValidos) {
        Reserva reserva = reservaService.iniciarAlquiler(codigo, agente, documentosValidos);
        Vehiculo vehiculo = vehiculoService.registrarEntrega(reserva.getVehiculoPlaca());
        return resultado(reserva, vehiculo);
    }

    /** HU-17: la reserva pasa a FINALIZADA y el vehículo vuelve a DISPONIBLE en la sucursal de devolución. */
    public ResultadoEntrega recibir(AgenteSucursal agente, String codigo, Double valorDano, String motivoDano) {
        Reserva reserva = reservaService.finalizarAlquiler(codigo, agente, valorDano, motivoDano);
        Vehiculo vehiculo = vehiculoService.registrarDevolucion(reserva.getVehiculoPlaca(),
                reserva.getSucursalDevolucionId());
        return resultado(reserva, vehiculo);
    }

    private static ResultadoEntrega resultado(Reserva reserva, Vehiculo vehiculo) {
        return new ResultadoEntrega(reserva.getCodigo(), reserva.getEstado(), vehiculo.getPlaca(),
                vehiculo.getEstado(), vehiculo.getSucursalId(), reserva.getPenalizacionDano());
    }
}
