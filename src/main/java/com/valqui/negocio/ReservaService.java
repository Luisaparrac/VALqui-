package com.valqui.negocio;

import com.valqui.datos.ReservaRepository;
import com.valqui.modelo.AgenteSucursal;
import com.valqui.modelo.Cobertura;
import com.valqui.modelo.CompaniaAliada;
import com.valqui.modelo.EstadoReserva;
import com.valqui.modelo.EstadoVehiculo;
import com.valqui.modelo.MetodoPago;
import com.valqui.modelo.Reserva;
import com.valqui.modelo.Vehiculo;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    private static final String CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final ReservaRepository reservaRepository;
    private final VehiculoService vehiculoService;
    private final UsuarioService usuarioService;
    private final CompaniaService companiaService;
    private final PagoService pagoService;

    public ReservaService(ReservaRepository reservaRepository, VehiculoService vehiculoService,
                          UsuarioService usuarioService, CompaniaService companiaService,
                          PagoService pagoService) {
        this.reservaRepository = reservaRepository;
        this.vehiculoService = vehiculoService;
        this.usuarioService = usuarioService;
        this.companiaService = companiaService;
        this.pagoService = pagoService;
    }

    /** HU-04: crea la reserva en PENDIENTE copiando las condiciones del vehículo y de la compañía. */
    public Reserva crearReserva(Reserva reserva) {
        if (!usuarioService.esMayorDeEdad(reserva.getClienteId())) {
            throw new NegocioException(403, "Debes ser mayor de edad para reservar");
        }
        LocalDateTime recogida = reserva.getFechaHoraRecogida();
        LocalDateTime devolucion = reserva.getFechaHoraDevolucion();
        if (recogida == null || devolucion == null || !recogida.isAfter(LocalDateTime.now())
                || !devolucion.isAfter(recogida)) {
            throw new NegocioException(400, "Fechas inválidas: la recogida debe ser futura y la devolución posterior a la recogida");
        }
        Vehiculo vehiculo = vehiculoService.consultarVehiculo(reserva.getVehiculoPlaca());
        String nit = vehiculo.getCompaniaNit();
        companiaService.consultarSucursal(nit, reserva.getSucursalRecogidaId());
        companiaService.consultarSucursal(nit, reserva.getSucursalDevolucionId());
        boolean estadoPermiteReserva = vehiculo.getEstado() == EstadoVehiculo.DISPONIBLE
                || vehiculo.getEstado() == EstadoVehiculo.RESERVADO;
        if (!estadoPermiteReserva
                || reservaRepository.existeReservaActiva(vehiculo.getPlaca(), recogida, devolucion)) {
            throw new NegocioException(409, "El vehículo no está disponible en esas fechas");
        }
        CompaniaAliada compania = companiaService.consultarCompania(nit);

        reserva.setCodigo(generarCodigo());
        reserva.setVehiculoPlaca(vehiculo.getPlaca());
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setTarifaDiaria(vehiculo.getTarifaDiaria());
        reserva.setKilometrajeIncluido(vehiculo.getKilometrajeIncluido());
        reserva.setFranquiciaDanos(vehiculo.getFranquiciaDanos());
        reserva.setValorDeposito(compania.getValorDeposito());
        return reservaRepository.save(reserva);
    }

    /** HU-05: solo en PENDIENTE y con una cobertura de la compañía dueña del vehículo. */
    public Reserva agregarCobertura(String codigo, String cobertura) {
        Reserva reserva = consultarReserva(codigo);
        String nit = vehiculoService.consultarVehiculo(reserva.getVehiculoPlaca()).getCompaniaNit();
        Cobertura encontrada = companiaService.consultarCoberturas(nit).stream()
                .filter(c -> c.getId().equals(cobertura))
                .findFirst()
                .orElseThrow(() -> new NegocioException(400, "La cobertura no pertenece a la compañía del vehículo"));
        reserva.agregarCobertura(encontrada);
        reservaRepository.save(reserva);
        return reserva;
    }

    /** HU-06: revisa la mayoría de edad (R16), cobra calcularTotal() y confirma. */
    public Reserva confirmar(String codigo, String ultimosDigitos) {
        Reserva reserva = consultarReserva(codigo);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new NegocioException(409, "Solo se puede confirmar una reserva pendiente");
        }
        if (!usuarioService.esMayorDeEdad(reserva.getClienteId())) {
            throw new NegocioException(403, "Debes ser mayor de edad para reservar");
        }
        MetodoPago metodoPago = usuarioService.obtenerMetodoPago(reserva.getClienteId(), ultimosDigitos);
        if (!pagoService.cobrar(reserva.getClienteId(), ultimosDigitos, reserva.calcularTotal())) {
            throw new NegocioException(409, "Pago rechazado");
        }
        reserva.confirmar(metodoPago);
        reservaRepository.save(reserva);
        return reserva;
    }

    /** HU-07 / R12: cancela y devuelve la penalización aplicada. */
    public double cancelar(String codigo) {
        Reserva reserva = consultarReserva(codigo);
        String nit = vehiculoService.consultarVehiculo(reserva.getVehiculoPlaca()).getCompaniaNit();
        CompaniaAliada compania = companiaService.consultarCompania(nit);
        double penalizacion = reserva.cancelar(LocalDateTime.now(), compania.getHorasCancelacionGratuita(),
                compania.getPenalizacionCancelacion());
        reservaRepository.save(reserva);
        return penalizacion;
    }

    /** HU-08 / R20 */
    public List<Reserva> consultarHistorial(String clienteId) {
        return reservaRepository.findByClienteId(clienteId).stream().map(this::cargarCoberturas).toList();
    }

    /** HU-16: el agente entrega el vehículo y la reserva pasa a EN_CURSO. */
    public Reserva iniciarAlquiler(String codigo, AgenteSucursal agente, boolean documentosValidos) {
        Reserva reserva = consultarReserva(codigo);
        agente.entregarVehiculo(reserva, documentosValidos);
        reservaRepository.save(reserva);
        return reserva;
    }

    /** HU-17: el agente recibe el vehículo, registra el daño (si hay) y la reserva pasa a FINALIZADA. */
    public Reserva finalizarAlquiler(String codigo, AgenteSucursal agente, Double valorDano, String motivoDano) {
        Reserva reserva = consultarReserva(codigo);
        agente.recibirVehiculo(reserva, valorDano, motivoDano);
        reservaRepository.save(reserva);
        return reserva;
    }

    public Reserva consultarReserva(String codigo) {
        return reservaRepository.findByCodigo(codigo)
                .map(this::cargarCoberturas)
                .orElseThrow(() -> new NegocioException(404, "No se encontró la reserva"));
    }

    private Reserva cargarCoberturas(Reserva reserva) {
        if (!reserva.getCoberturaIds().isEmpty()) {
            String nit = vehiculoService.consultarVehiculo(reserva.getVehiculoPlaca()).getCompaniaNit();
            reserva.setCoberturas(companiaService.consultarCoberturas(nit).stream()
                    .filter(c -> reserva.getCoberturaIds().contains(c.getId()))
                    .toList());
        }
        return reserva;
    }

    private String generarCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("RES-");
            for (int i = 0; i < 6; i++) {
                sb.append(CARACTERES_CODIGO.charAt(ALEATORIO.nextInt(CARACTERES_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (reservaRepository.existsByCodigo(codigo));
        return codigo;
    }
}
