package com.valqui.presentacion;

import com.valqui.modelo.EstadoReserva;
import com.valqui.modelo.Reserva;
import com.valqui.modelo.Usuario;
import com.valqui.negocio.NegocioException;
import com.valqui.negocio.ReservaService;
import com.valqui.presentacion.dto.Peticiones;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /** HU-04 */
    @PostMapping
    public ResponseEntity<Respuestas.ReservaDetalle> crearReserva(@Valid @RequestBody Peticiones.NuevaReserva datos,
                                                                  @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        Reserva reserva = new Reserva();
        reserva.setClienteId(usuario.getId());
        reserva.setVehiculoPlaca(datos.placa());
        reserva.setFechaHoraRecogida(datos.fechaHoraRecogida());
        reserva.setFechaHoraDevolucion(datos.fechaHoraDevolucion());
        reserva.setSucursalRecogidaId(datos.sucursalRecogidaId());
        reserva.setSucursalDevolucionId(datos.sucursalDevolucionId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Respuestas.ReservaDetalle.de(reservaService.crearReserva(reserva)));
    }

    /** HU-05 */
    @PostMapping("/{codigo}/coberturas")
    public ResponseEntity<Respuestas.ReservaDetalle> agregarCobertura(@PathVariable String codigo,
                                                                      @Valid @RequestBody Peticiones.AgregarCobertura datos,
                                                                      @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        verificarDueno(usuario, codigo);
        return ResponseEntity.ok(Respuestas.ReservaDetalle.de(reservaService.agregarCobertura(codigo, datos.coberturaId())));
    }

    /** HU-06 */
    @PostMapping("/{codigo}/confirmar")
    public ResponseEntity<Respuestas.ReservaDetalle> confirmar(@PathVariable String codigo,
                                                               @Valid @RequestBody Peticiones.Confirmacion datos,
                                                               @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        verificarDueno(usuario, codigo);
        return ResponseEntity.ok(Respuestas.ReservaDetalle.de(reservaService.confirmar(codigo, datos.ultimosDigitos())));
    }

    /** HU-07 / R12 */
    @PostMapping("/{codigo}/cancelar")
    public ResponseEntity<Respuestas.Cancelacion> cancelar(@PathVariable String codigo,
                                                           @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        verificarDueno(usuario, codigo);
        double penalizacion = reservaService.cancelar(codigo);
        return ResponseEntity.ok(new Respuestas.Cancelacion(codigo, EstadoReserva.CANCELADA, penalizacion));
    }

    /** HU-08 / R20 */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Respuestas.ReservaDetalle>> consultarHistorial(@PathVariable String clienteId,
                                                                             @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        if (!usuario.getId().equals(clienteId)) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
        return ResponseEntity.ok(reservaService.consultarHistorial(clienteId).stream()
                .map(Respuestas.ReservaDetalle::de).toList());
    }

    private void verificarDueno(Usuario usuario, String codigo) {
        if (!reservaService.consultarReserva(codigo).getClienteId().equals(usuario.getId())) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
    }
}
