package com.valqui.presentacion;

import com.valqui.modelo.Cliente;
import com.valqui.modelo.MetodoPago;
import com.valqui.modelo.Usuario;
import com.valqui.negocio.NegocioException;
import com.valqui.negocio.UsuarioService;
import com.valqui.presentacion.dto.Peticiones;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** HU-01 */
    @PostMapping("/api/usuarios/clientes")
    public ResponseEntity<Respuestas.UsuarioRegistrado> registrarCliente(@Valid @RequestBody Peticiones.RegistroCliente cliente) {
        Cliente nuevo = new Cliente();
        nuevo.setNombreCompleto(cliente.nombreCompleto().trim());
        nuevo.setCorreo(cliente.correo());
        nuevo.setFechaNacimiento(cliente.fechaNacimiento());
        nuevo.setNumeroContacto(cliente.numeroContacto());
        nuevo.setPaisResidencia(cliente.paisResidencia());
        Cliente registrado = usuarioService.registrarCliente(nuevo, cliente.contrasena());
        return ResponseEntity.status(HttpStatus.CREATED).body(Respuestas.UsuarioRegistrado.de(registrado));
    }

    /** HU-18 */
    @PostMapping("/api/auth/login")
    public ResponseEntity<Respuestas.Sesion> iniciarSesion(@Valid @RequestBody Peticiones.Login login) {
        String token = usuarioService.autenticar(login.correo(), login.contrasena());
        Usuario usuario = usuarioService.validarSesion(token);
        return ResponseEntity.ok(new Respuestas.Sesion(token, usuario.getTipo(), usuario.getId()));
    }

    /** R03: solo el mismo cliente puede registrar sus métodos de pago. */
    @PostMapping("/api/clientes/{clienteId}/metodos-pago")
    public ResponseEntity<Respuestas.MetodoPagoRegistrado> registrarMetodoPago(
            @PathVariable String clienteId,
            @Valid @RequestBody Peticiones.RegistroMetodoPago metodoPago,
            @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        if (!usuario.getId().equals(clienteId)) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
        MetodoPago nuevo = new MetodoPago(metodoPago.marca(), metodoPago.tipo(),
                metodoPago.ultimosDigitos(), metodoPago.token());
        usuarioService.agregarMetodoPago(clienteId, nuevo);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Respuestas.MetodoPagoRegistrado(
                nuevo.getMarca().trim().toUpperCase(), nuevo.getTipo(), nuevo.getUltimosDigitos(),
                "Método de pago registrado"));
    }
}
