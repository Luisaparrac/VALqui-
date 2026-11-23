package com.valqui.negocio;

import com.valqui.datos.UsuarioRepository;
import com.valqui.modelo.Cliente;
import com.valqui.modelo.MetodoPago;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Usuario;
import com.valqui.modelo.UsuarioCompania;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class UsuarioService {

    /** Atributo del contexto donde FiltroSesion deja al usuario autenticado. */
    public static final String ATRIBUTO_SESION = "usuarioSesion";

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

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

    /** HU-01: registra un cliente nuevo y guarda su contraseña cifrada. */
    public Cliente registrarCliente(Cliente cliente) {
        cliente.setCorreo(normalizarCorreo(cliente.getCorreo()));
        if (usuarioRepository.findByCorreo(cliente.getCorreo()).isPresent()) {
            throw new NegocioException(409, "El correo ya está registrado");
        }
        cliente.setContrasena(BCRYPT.encode(cliente.getContrasena()));
        return guardarNuevo(cliente, "El correo ya está registrado");
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
            throw new NegocioException(400, "Franquicia de tarjeta no soportada");
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
     * HU-15: crea la cuenta de acceso de la compañía. La contraseña inicial es el NIT (cifrado con BCrypt),
     * el nombre completo es el correo, y cargo y teléfono quedan vacíos.
     */
    public UsuarioCompania crearCuentaCompania(String nit, String correo) {
        String correoNormalizado = normalizarCorreo(correo);
        if (usuarioRepository.findByCorreo(correoNormalizado).isPresent()) {
            throw new NegocioException(409, "Ya existe un usuario registrado con ese correo");
        }
        UsuarioCompania cuenta = new UsuarioCompania();
        cuenta.setNombreCompleto(correoNormalizado);
        cuenta.setCargo("");
        cuenta.setTelefono("");
        cuenta.setCompaniaNit(nit);
        cuenta.setCorreo(correoNormalizado);
        cuenta.setContrasena(BCRYPT.encode(nit));
        return guardarNuevo(cuenta, "Ya existe un usuario registrado con ese correo");
    }

    private Cliente buscarCliente(String clienteId) {
        return usuarioRepository.findById(clienteId)
                .filter(Cliente.class::isInstance)
                .map(Cliente.class::cast)
                .orElseThrow(() -> new NegocioException(404, "No se encontró el cliente"));
    }

    private <T extends Usuario> T guardarNuevo(T usuario, String mensajeDuplicado) {
        try {
            return usuarioRepository.save(usuario);
        } catch (DuplicateKeyException e) {
            throw new NegocioException(409, mensajeDuplicado);
        }
    }

    private static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase();
    }
}
