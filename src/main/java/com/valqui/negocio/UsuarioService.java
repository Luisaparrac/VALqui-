package com.valqui.negocio;

import com.valqui.datos.UsuarioRepository;
import com.valqui.modelo.Cliente;
import com.valqui.modelo.MetodoPago;
import com.valqui.modelo.Usuario;
import com.valqui.modelo.UsuarioCompania;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class UsuarioService {

    private static final String CARACTERES_CONTRASENA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final SecretKey claveJwt;
    private final long horasExpiracion;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          @Value("${valqui.jwt.secreto}") String secreto,
                          @Value("${valqui.jwt.horas-expiracion}") long horasExpiracion) {
        this.usuarioRepository = usuarioRepository;
        this.claveJwt = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.horasExpiracion = horasExpiracion;
    }

    /** HU-01: registra un cliente nuevo. */
    public Cliente registrarCliente(Cliente cliente, String contrasena) {
        cliente.setCorreo(normalizarCorreo(cliente.getCorreo()));
        if (usuarioRepository.findByCorreo(cliente.getCorreo()).isPresent()) {
            throw new NegocioException(409, "Ya existe un usuario registrado con ese correo");
        }
        cliente.setContrasena(contrasena);
        return guardarNuevo(cliente);
    }

    /** HU-18: valida las credenciales y devuelve el token de sesión. */
    public String autenticar(String correo, String contrasena) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(correo))
                .filter(u -> u.iniciarSesion(normalizarCorreo(correo), contrasena))
                .orElseThrow(() -> new NegocioException(401, "Correo o contraseña incorrectos"));
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId())
                .claim("correo", usuario.getCorreo())
                .claim("tipo", usuario.getTipo())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(horasExpiracion, ChronoUnit.HOURS)))
                .signWith(claveJwt)
                .compact();
    }

    /** Valida el token JWT y devuelve el usuario de la sesión. */
    public Usuario validarSesion(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(claveJwt).build().parseSignedClaims(token).getPayload();
            return usuarioRepository.findById(claims.getSubject())
                    .filter(u -> u.getTipo().equals(claims.get("tipo", String.class)))
                    .orElseThrow(() -> new NegocioException(401, "Sesión inválida o expirada"));
        } catch (JwtException | IllegalArgumentException e) {
            throw new NegocioException(401, "Sesión inválida o expirada");
        }
    }

    /** R16: verifica la mayoría de edad del cliente. */
    public boolean esMayorDeEdad(String clienteId) {
        return buscarCliente(clienteId).esMayorDeEdad();
    }

    /** R03: registra un método de pago embebido en el cliente. */
    public void agregarMetodoPago(String clienteId, MetodoPago metodoPago) {
        if (!metodoPago.esMarcaValida()) {
            throw new NegocioException(400, "Marca de tarjeta no aceptada");
        }
        Cliente cliente = buscarCliente(clienteId);
        boolean repetido = cliente.getMetodosPago().stream()
                .anyMatch(m -> m.getUltimosDigitos().equals(metodoPago.getUltimosDigitos()));
        if (repetido) {
            throw new NegocioException(409, "Ya tienes registrado un método de pago con esos últimos dígitos");
        }
        cliente.agregarMetodoPago(new MetodoPago(metodoPago.getMarca().trim().toUpperCase(),
                metodoPago.getTipo(), metodoPago.getUltimosDigitos(), metodoPago.getToken()));
        usuarioRepository.save(cliente);
    }

    public MetodoPago obtenerMetodoPago(String clienteId, String ultimosDigitos) {
        return buscarCliente(clienteId).getMetodosPago().stream()
                .filter(m -> m.getUltimosDigitos().equals(ultimosDigitos))
                .findFirst()
                .orElseThrow(() -> new NegocioException(400, "Debes registrar un método de pago"));
    }

    /**
     * HU-15: crea la cuenta de acceso de la compañía con una contraseña temporal generada.
     * Devuelve la contraseña temporal en claro (solo se muestra una vez).
     */
    public String crearCuentaCompania(String nit, String correo, UsuarioCompania representante) {
        String correoNormalizado = normalizarCorreo(correo);
        if (usuarioRepository.findByCorreo(correoNormalizado).isPresent()) {
            throw new NegocioException(409, "Ya existe un usuario registrado con ese correo");
        }
        String contrasenaTemporal = generarContrasenaTemporal();
        representante.setCompaniaNit(nit);
        representante.setCorreo(correoNormalizado);
        representante.setContrasena(contrasenaTemporal);
        guardarNuevo(representante);
        return contrasenaTemporal;
    }

    private Cliente buscarCliente(String clienteId) {
        return usuarioRepository.findById(clienteId)
                .filter(Cliente.class::isInstance)
                .map(Cliente.class::cast)
                .orElseThrow(() -> new NegocioException(404, "No se encontró el cliente"));
    }

    private <T extends Usuario> T guardarNuevo(T usuario) {
        try {
            return usuarioRepository.save(usuario);
        } catch (DuplicateKeyException e) {
            throw new NegocioException(409, "Ya existe un usuario registrado con ese correo");
        }
    }

    private static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase();
    }

    private static String generarContrasenaTemporal() {
        StringBuilder sb = new StringBuilder("Tmp-");
        for (int i = 0; i < 10; i++) {
            sb.append(CARACTERES_CONTRASENA.charAt(ALEATORIO.nextInt(CARACTERES_CONTRASENA.length())));
        }
        return sb.append('*').toString();
    }
}
