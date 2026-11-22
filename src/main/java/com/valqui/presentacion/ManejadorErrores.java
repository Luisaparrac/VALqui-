package com.valqui.presentacion;

import com.valqui.negocio.NegocioException;
import com.valqui.presentacion.dto.Respuestas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/** Convierte todas las excepciones al formato { "mensaje": "...", "codigo": N }. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Respuestas.Error> negocio(NegocioException e) {
        return error(e.getCodigo(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Respuestas.Error> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return error(400, "Datos inválidos: " + detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Respuestas.Error> cuerpoInvalido(HttpMessageNotReadableException e) {
        return error(400, "Datos inválidos: el cuerpo de la petición no tiene el formato esperado");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Respuestas.Error> parametroInvalido(Exception e) {
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

    private static ResponseEntity<Respuestas.Error> error(int codigo, String mensaje) {
        return ResponseEntity.status(codigo).body(new Respuestas.Error(mensaje, codigo));
    }
}
