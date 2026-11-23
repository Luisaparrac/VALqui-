package com.valqui.negocio;

import com.valqui.datos.ReservaRepository;
import com.valqui.modelo.AgenteSucursal;
import com.valqui.modelo.Cobertura;
import com.valqui.modelo.CompaniaAliada;
import com.valqui.modelo.EstadoReserva;
import com.valqui.modelo.MetodoPago;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Reserva;
import com.valqui.modelo.Usuario;
import com.valqui.modelo.Vehiculo;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    /** La firma de iniciarAlquiler no trae este dato: EntregaService lo deja en la petición. */
    static final String ATRIBUTO_DOCUMENTOS = "documentosValidos";

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

    /** HU-04: guarda la reserva copiando las condiciones del vehículo y de la compañía. */
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
        reserva.setSucursalRecogida(companiaService.consultarSucursal(nit, reserva.getSucursalRecogida()).getNombre());
        reserva.setSucursalDevolucion(companiaService.consultarSucursal(nit, reserva.getSucursalDevolucion()).getNombre());
        if (!vehiculo.estaDisponible(recogida, devolucion)
                || reservaRepository.existeReservaActiva(vehiculo.getPlaca(), recogida, devolucion)) {
            throw new NegocioException(409, "El vehículo no está disponible en las fechas seleccionadas");
        }
        CompaniaAliada compania = vehiculo.getCompania();

        reserva.setCodigo(generarCodigo());
        reserva.setVehiculoPlaca(vehiculo.getPlaca());
        reserva.setTarifaDiaria(vehiculo.getTarifaDiaria());
        reserva.setKilometrajeIncluido(vehiculo.getKilometrajeIncluido());
        reserva.setFranquiciaDanos(vehiculo.getFranquiciaDanos());
        reserva.setValorDeposito(compania.getValorDeposito());
        return reservaRepository.insert(reserva);
    }

    /** HU-05: solo en PENDIENTE y con una cobertura de la compañía dueña del vehículo. */
    public Reserva agregarCobertura(String codigo, String cobertura) {
        Reserva reserva = buscarReserva(codigo);
        String nit = vehiculoService.consultarVehiculo(reserva.getVehiculoPlaca()).getCompaniaNit();
        String buscada = cobertura == null ? "" : cobertura.trim();
        Cobertura encontrada = companiaService.consultarCoberturas(nit).stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(buscada))
                .findFirst()
                .orElseThrow(() -> new NegocioException(400, "La cobertura no pertenece a la compañía del vehículo"));
        reserva.agregarCobertura(encontrada);
        reservaRepository.save(reserva);
        return reserva;
    }

    /** HU-06: revisa la mayoría de edad (R16), cobra calcularTotal() y confirma. */
    public Reserva confirmar(String codigo, String ultimosDigitos) {
        Reserva reserva = buscarReserva(codigo);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new NegocioException(409, "Solo se puede confirmar una reserva pendiente");
        }
        if (!usuarioService.esMayorDeEdad(reserva.getClienteId())) {
            throw new NegocioException(403, "Debes ser mayor de edad para reservar");
        }
        MetodoPago metodoPago = usuarioService.obtenerMetodoPago(reserva.getClienteId(), ultimosDigitos);
        if (!pagoService.cobrar(reserva.getClienteId(), ultimosDigitos, reserva.calcularTotal())) {
            throw new NegocioException(409, "No fue posible procesar el pago, intenta con otro método");
        }
        reserva.confirmar(metodoPago);
        reservaRepository.save(reserva);
        return reserva;
    }

    /**
     * HU-07 / R12: aplica la política vigente de la compañía. Si hay penalización, la cobra con el
     * método de pago de la confirmación; si el cobro falla, la reserva no se cancela.
     */
    public Reserva cancelar(String codigo) {
        Reserva reserva = buscarReserva(codigo);
        String nit = vehiculoService.consultarVehiculo(reserva.getVehiculoPlaca()).getCompaniaNit();
        CompaniaAliada compania = companiaService.consultarCompania(nit);
        reserva.setHorasCancelacionGratuita(compania.getHorasCancelacionGratuita());
        reserva.setPenalizacionCancelacion(compania.getPenalizacionCancelacion());
        double penalizacion = reserva.cancelar(LocalDateTime.now());
        if (penalizacion > 0
                && !pagoService.cobrar(reserva.getClienteId(), reserva.getMetodoPago(), penalizacion)) {
            throw new NegocioException(409, "No fue posible procesar el pago, intenta con otro método");
        }
        reservaRepository.save(reserva);
        return reserva;
    }

    /** HU-08 / R20 */
    public List<Reserva> consultarHistorial(String clienteId) {
        return reservaRepository.findByClienteId(clienteId).stream().map(this::cargarCoberturas).toList();
    }

    /** HU-16: el agente de la sesión entrega el vehículo y la reserva pasa a EN_CURSO. */
    public Reserva iniciarAlquiler(String codigo) {
        Reserva reserva = buscarReserva(codigo);
        boolean documentosValidos = Boolean.TRUE.equals(atributoPeticion(ATRIBUTO_DOCUMENTOS));
        agenteSesion().entregarVehiculo(reserva, documentosValidos);
        reservaRepository.save(reserva);
        return reserva;
    }

    /** HU-17: el agente de la sesión recibe el vehículo, registra el daño (si hay) y la reserva pasa a FINALIZADA. */
    public Reserva finalizarAlquiler(String codigo, double valorDano, String motivoDano) {
        Reserva reserva = buscarReserva(codigo);
        agenteSesion().recibirVehiculo(reserva, valorDano, motivoDano);
        reservaRepository.save(reserva);
        return reserva;
    }

    private Reserva buscarReserva(String codigo) {
        return reservaRepository.findById(codigo)
                .map(this::cargarCoberturas)
                .orElseThrow(() -> new NegocioException(404, "No se encontró la reserva"));
    }

    private Reserva cargarCoberturas(Reserva reserva) {
        if (!reserva.getCoberturaNombres().isEmpty()) {
            String nit = vehiculoService.consultarVehiculo(reserva.getVehiculoPlaca()).getCompaniaNit();
            reserva.setCoberturas(companiaService.consultarCoberturas(nit).stream()
                    .filter(c -> reserva.getCoberturaNombres().contains(c.getNombre()))
                    .toList());
        }
        return reserva;
    }

    private AgenteSucursal agenteSesion() {
        return (AgenteSucursal) atributoPeticion(Usuario.ATRIBUTO_SESION);
    }

    private static Object atributoPeticion(String nombre) {
        return RequestContextHolder.currentRequestAttributes().getAttribute(nombre, RequestAttributes.SCOPE_REQUEST);
    }

    private String generarCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("RES-");
            for (int i = 0; i < 6; i++) {
                sb.append(CARACTERES_CODIGO.charAt(ALEATORIO.nextInt(CARACTERES_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (reservaRepository.existsById(codigo));
        return codigo;
    }
}
