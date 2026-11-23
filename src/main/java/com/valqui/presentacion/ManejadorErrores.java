package com.valqui.presentacion;

import com.valqui.modelo.NegocioException;
import com.valqui.presentacion.dto.Respuestas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Convierte todas las excepciones al formato { "mensaje": "...", "codigo": N }. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Las HU fijan estos mensajes completos: se responden sin el prefijo "Datos inválidos". */
    private static final String PREFIJO_MENSAJE_HU = "El campo ";

    private static final Map<String, String> PARAMETROS_OBLIGATORIOS = Map.ofEntries(
            Map.entry("correo", "El correo es obligatorio"),
            Map.entry("contrasena", "La contraseña es obligatoria"),
            Map.entry("correoAcceso", "El correo de acceso es obligatorio"),
            Map.entry("horasGratis", "Las horas de cancelación gratuita son obligatorias"),
            Map.entry("penalizacion", "La penalización de cancelación es obligatoria"),
            Map.entry("deposito", "El valor del depósito es obligatorio"),
            Map.entry("sucursal", "La sucursal es obligatoria"),
            Map.entry("estado", "El estado es obligatorio"),
            Map.entry("cobertura", "La cobertura es obligatoria"),
            Map.entry("ultimosDigitos", "Debes registrar un método de pago"),
            Map.entry("documentosValidos", "Debe indicar si los documentos son válidos"));

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Respuestas.Error> negocio(NegocioException e) {
        return error(e.getCodigo(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Respuestas.Error> validacion(MethodArgumentNotValidException e) {
        return errorValidacion(e.getBindingResult().getAllErrors().stream().map(MessageSourceResolvable::getDefaultMessage));
    }

    /** Validaciones sobre @RequestParam (y el @RequestBody del mismo método). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Respuestas.Error> validacionParametros(HandlerMethodValidationException e) {
        return errorValidacion(e.getAllErrors().stream().map(MessageSourceResolvable::getDefaultMessage));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Respuestas.Error> cuerpoInvalido(HttpMessageNotReadableException e) {
        return error(400, "Datos inválidos: el cuerpo de la petición no tiene el formato esperado");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Respuestas.Error> parametroFaltante(MissingServletRequestParameterException e) {
        String mensaje = PARAMETROS_OBLIGATORIOS.get(e.getParameterName());
        return error(400, "Datos inválidos: " + (mensaje != null ? mensaje : "parámetros de la petición incorrectos"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Respuestas.Error> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return error(400, "Datos inválidos: parámetros de la petición incorrectos");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Respuestas.Error> duplicado(DuplicateKeyException e) {
        return error(409, "El registro ya existe");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Respuestas.Error> noEncontrado(NoResourceFoundException e) {
        return error(404, "Recurso no encontrado");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Respuestas.Error> metodoNoSoportado(HttpRequestMethodNotSupportedException e) {
        return error(404, "Recurso no encontrado");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Respuestas.Error> general(Exception e) {
        log.error("Error no controlado", e);
        return error(500, "Error interno del servidor");
    }

    private static ResponseEntity<Respuestas.Error> errorValidacion(Stream<String> mensajes) {
        List<String> distintos = mensajes.distinct().toList();
        return distintos.stream()
                .filter(m -> m != null && m.startsWith(PREFIJO_MENSAJE_HU))
                .findFirst()
                .map(m -> error(400, m))
                .orElseGet(() -> error(400, "Datos inválidos: " + String.join("; ", distintos)));
    }

    private static ResponseEntity<Respuestas.Error> error(int codigo, String mensaje) {
        return ResponseEntity.status(codigo).body(new Respuestas.Error(mensaje, codigo));
    }
}
