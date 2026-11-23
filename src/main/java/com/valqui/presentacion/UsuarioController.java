package com.valqui.presentacion;

import com.valqui.modelo.Cliente;
import com.valqui.modelo.MetodoPago;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Usuario;
import com.valqui.negocio.UsuarioService;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** HU-01 */
    @PostMapping("/api/usuarios/clientes")
    public ResponseEntity<Respuestas.UsuarioRegistrado> registrarCliente(@Valid @RequestBody Cliente cliente) {
        cliente.setNombreCompleto(cliente.getNombreCompleto().trim());
        Cliente registrado = usuarioService.registrarCliente(cliente);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Respuestas.UsuarioRegistrado.de(registrado, "Registro exitoso"));
    }

    /** HU-18 */
    @PostMapping("/api/auth/login")
    public ResponseEntity<Respuestas.Sesion> iniciarSesion(
            @RequestParam @NotBlank(message = "El correo es obligatorio") String correo,
            @RequestParam @NotBlank(message = "La contraseña es obligatoria") String contrasena) {
        String token = usuarioService.autenticar(correo, contrasena);
        Usuario usuario = usuarioService.validarSesion(token);
        return ResponseEntity.ok(new Respuestas.Sesion(token, usuario.getTipo(), usuario.getId()));
    }

    /** R03: solo el mismo cliente puede registrar sus métodos de pago. */
    @PostMapping("/api/clientes/{clienteId}/metodos-pago")
    public ResponseEntity<Respuestas.MetodoPagoRegistrado> registrarMetodoPago(
            @PathVariable String clienteId,
            @Valid @RequestBody MetodoPago metodoPago) {
        if (!usuarioSesion().getId().equals(clienteId)) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
        usuarioService.agregarMetodoPago(clienteId, metodoPago);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Respuestas.MetodoPagoRegistrado(
                metodoPago.getMarca().trim().toUpperCase(), metodoPago.getTipo(), metodoPago.getUltimosDigitos(),
                "Método de pago registrado"));
    }

    private static Usuario usuarioSesion() {
        return (Usuario) RequestContextHolder.currentRequestAttributes()
                .getAttribute(FiltroSesion.ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST);
    }
}
