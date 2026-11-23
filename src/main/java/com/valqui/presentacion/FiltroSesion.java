package com.valqui.presentacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Usuario;
import com.valqui.negocio.UsuarioService;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * «Filter»: revisa "Authorization: Bearer &lt;token&gt;" en todas las rutas excepto
 * POST /api/auth/login y POST /api/usuarios/clientes. 401 sin token o token inválido; 403 si el rol no corresponde.
 */
@Component
public class FiltroSesion extends OncePerRequestFilter {

    public static final String ATRIBUTO_USUARIO = Usuario.ATRIBUTO_SESION;

    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper;

    public FiltroSesion(UsuarioService usuarioService, ObjectMapper objectMapper) {
        this.usuarioService = usuarioService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String metodo = request.getMethod();
        String ruta = request.getRequestURI().substring(request.getContextPath().length());

        if (esPublica(metodo, ruta)) {
            chain.doFilter(request, response);
            return;
        }
        String encabezado = request.getHeader("Authorization");
        String token = encabezado != null && encabezado.startsWith("Bearer ") ? encabezado.substring(7).trim() : null;
        if (token == null || token.isEmpty() || !validarPeticion(token)) {
            responderError(response, 401, "Debes iniciar sesión para acceder a este recurso");
            return;
        }
        Usuario usuario = (Usuario) request.getAttribute(ATRIBUTO_USUARIO);
        String rolRequerido = rolRequerido(metodo, ruta);
        if (rolRequerido != null && !rolRequerido.equals(usuario.getTipo())) {
            responderError(response, 403, "No tienes permiso para realizar esta acción");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Valida el token una sola vez y deja al usuario de la sesión en la petición. */
    public boolean validarPeticion(String token) {
        try {
            Usuario usuario = usuarioService.validarSesion(token);
            RequestContextHolder.currentRequestAttributes()
                    .setAttribute(ATRIBUTO_USUARIO, usuario, RequestAttributes.SCOPE_REQUEST);
            return true;
        } catch (NegocioException e) {
            return false;
        }
    }

    private static boolean esPublica(String metodo, String ruta) {
        return "POST".equals(metodo) && (ruta.equals("/api/auth/login") || ruta.equals("/api/usuarios/clientes"));
    }

    /** Rol que exige cada ruta (null = cualquier usuario con sesión). */
    private static String rolRequerido(String metodo, String ruta) {
        if (ruta.startsWith("/api/clientes/") || ruta.startsWith("/api/reservas")) {
            return Usuario.CLIENTE;
        }
        if (ruta.equals("/api/companias") || ruta.equals("/api/companias/")) {
            return Usuario.ADMIN;
        }
        if (ruta.startsWith("/api/companias/")) {
            return Usuario.COMPANIA;
        }
        if (ruta.startsWith("/api/vehiculos")) {
            return "GET".equals(metodo) ? Usuario.CLIENTE : Usuario.COMPANIA;
        }
        if (ruta.startsWith("/api/entregas")) {
            return Usuario.AGENTE;
        }
        return null;
    }

    private void responderError(HttpServletResponse response, int codigo, String mensaje) throws IOException {
        response.setStatus(codigo);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new Respuestas.Error(mensaje, codigo));
    }
}
