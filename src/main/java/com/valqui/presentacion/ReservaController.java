package com.valqui.presentacion;

import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Reserva;
import com.valqui.modelo.Usuario;
import com.valqui.negocio.ReservaService;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

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
    public ResponseEntity<Respuestas.ReservaDetalle> crearReserva(@Valid @RequestBody Reserva reserva) {
        reserva.setClienteId(usuarioSesion().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Respuestas.ReservaDetalle.de(reservaService.crearReserva(reserva)));
    }

    /** HU-05 */
    @PostMapping("/{codigo}/coberturas")
    public ResponseEntity<Respuestas.ReservaDetalle> agregarCobertura(
            @PathVariable String codigo,
            @RequestParam @NotBlank(message = "La cobertura es obligatoria") String cobertura) {
        verificarDueno(codigo);
        return ResponseEntity.ok(Respuestas.ReservaDetalle.de(reservaService.agregarCobertura(codigo, cobertura)));
    }

    /** HU-06 */
    @PostMapping("/{codigo}/confirmar")
    public ResponseEntity<Respuestas.ReservaDetalle> confirmar(
            @PathVariable String codigo,
            @RequestParam @NotBlank(message = "Debes registrar un método de pago") String ultimosDigitos) {
        verificarDueno(codigo);
        return ResponseEntity.ok(Respuestas.ReservaDetalle.de(reservaService.confirmar(codigo, ultimosDigitos)));
    }

    /** HU-07 / R12 */
    @PostMapping("/{codigo}/cancelar")
    public ResponseEntity<Respuestas.Cancelacion> cancelar(@PathVariable String codigo) {
        verificarDueno(codigo);
        Reserva reserva = reservaService.cancelar(codigo);
        return ResponseEntity.ok(new Respuestas.Cancelacion(reserva.getCodigo(), reserva.getEstado(),
                reserva.getPenalizacionAplicada()));
    }

    /** HU-08 / R20 */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<Respuestas.Historial> consultarHistorial(@PathVariable String clienteId) {
        if (!usuarioSesion().getId().equals(clienteId)) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
        List<Respuestas.ReservaDetalle> reservas = reservaService.consultarHistorial(clienteId).stream()
                .map(Respuestas.ReservaDetalle::de).toList();
        return ResponseEntity.ok(new Respuestas.Historial(reservas, reservas.isEmpty() ? "Aún no tienes reservas" : null));
    }

    /** Una reserva que no está en el historial del cliente de la sesión no existe para él. */
    private void verificarDueno(String codigo) {
        boolean esSuya = reservaService.consultarHistorial(usuarioSesion().getId()).stream()
                .anyMatch(r -> r.getCodigo().equals(codigo));
        if (!esSuya) {
            throw new NegocioException(404, "No se encontró la reserva");
        }
    }

    private static Usuario usuarioSesion() {
        return (Usuario) RequestContextHolder.currentRequestAttributes()
                .getAttribute(FiltroSesion.ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST);
    }
}
