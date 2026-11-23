package com.valqui.presentacion;

import com.valqui.modelo.EstadoVehiculo;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Usuario;
import com.valqui.modelo.UsuarioCompania;
import com.valqui.modelo.Vehiculo;
import com.valqui.negocio.VehiculoService;
import com.valqui.presentacion.dto.DetalleVehiculo;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    /** HU-10 (COMPANIA) */
    @PostMapping
    public ResponseEntity<Vehiculo> registrarVehiculo(@Valid @RequestBody Vehiculo vehiculo) {
        vehiculo.setCompaniaNit(((UsuarioCompania) usuarioSesion()).getCompaniaNit());
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoService.registrarVehiculo(vehiculo));
    }

    /** HU-02 (CLIENTE) */
    @GetMapping("/disponibles")
    public ResponseEntity<Respuestas.Busqueda> buscarDisponibles(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String nit) {
        List<Vehiculo> vehiculos = vehiculoService.buscarDisponibles(ciudad, desde, hasta, categoria, nit);
        String mensaje = vehiculos.isEmpty() ? "No se encontraron vehículos disponibles con los filtros seleccionados" : null;
        return ResponseEntity.ok(new Respuestas.Busqueda(vehiculos, mensaje));
    }

    /** HU-03 / R15 (CLIENTE) */
    @GetMapping("/{placa}")
    public ResponseEntity<DetalleVehiculo> consultarDetalle(@PathVariable String placa) {
        return ResponseEntity.ok(DetalleVehiculo.de(vehiculoService.consultarVehiculo(placa)));
    }

    /** HU-11 (COMPANIA) */
    @PutMapping("/{placa}/sucursal")
    public ResponseEntity<Vehiculo> reasignar(@PathVariable String placa,
                                              @RequestParam @NotBlank(message = "La sucursal es obligatoria") String sucursal) {
        verificarVehiculoPropio(placa);
        vehiculoService.reasignar(placa, sucursal);
        return ResponseEntity.ok(vehiculoService.consultarVehiculo(placa));
    }

    /** HU-14 (COMPANIA): RESERVADO lo asigna el sistema al entregar el vehículo. */
    @PutMapping("/{placa}/estado")
    public ResponseEntity<Vehiculo> cambiarEstado(@PathVariable String placa,
                                                  @RequestParam EstadoVehiculo estado) {
        verificarVehiculoPropio(placa);
        if (estado == EstadoVehiculo.RESERVADO) {
            throw new NegocioException(400, "El estado RESERVADO lo asigna el sistema, no se puede asignar manualmente");
        }
        vehiculoService.cambiarEstado(placa, estado);
        return ResponseEntity.ok(vehiculoService.consultarVehiculo(placa));
    }

    private void verificarVehiculoPropio(String placa) {
        Vehiculo vehiculo = vehiculoService.consultarVehiculo(placa);
        if (!vehiculo.getCompaniaNit().equals(((UsuarioCompania) usuarioSesion()).getCompaniaNit())) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
    }

    private static Usuario usuarioSesion() {
        return (Usuario) RequestContextHolder.currentRequestAttributes()
                .getAttribute(FiltroSesion.ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST);
    }
}
