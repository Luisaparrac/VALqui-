package com.valqui.presentacion;

import com.valqui.modelo.Usuario;
import com.valqui.modelo.UsuarioCompania;
import com.valqui.modelo.Vehiculo;
import com.valqui.negocio.DetalleVehiculo;
import com.valqui.negocio.NegocioException;
import com.valqui.negocio.VehiculoService;
import com.valqui.presentacion.dto.Peticiones;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Vehiculo> registrarVehiculo(@Valid @RequestBody Peticiones.RegistroVehiculo datos,
                                                      @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(datos.placa());
        vehiculo.setMarca(datos.marca());
        vehiculo.setModelo(datos.modelo());
        vehiculo.setCategoria(datos.categoria());
        vehiculo.setAnio(datos.anio());
        vehiculo.setTransmision(datos.transmision());
        vehiculo.setCapacidadPasajeros(datos.capacidadPasajeros());
        vehiculo.setTarifaDiaria(datos.tarifaDiaria());
        vehiculo.setKilometrajeIncluido(datos.kilometrajeIncluido());
        vehiculo.setFranquiciaDanos(datos.franquiciaDanos());
        vehiculo.setCompaniaNit(((UsuarioCompania) usuario).getCompaniaNit());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehiculoService.registrarVehiculo(vehiculo, datos.sucursalId()));
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
        return ResponseEntity.ok(vehiculoService.consultarDetalle(placa));
    }

    /** HU-11 (COMPANIA) */
    @PutMapping("/{placa}/sucursal")
    public ResponseEntity<Vehiculo> reasignar(@PathVariable String placa,
                                              @Valid @RequestBody Peticiones.Reasignacion datos,
                                              @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        verificarVehiculoPropio(usuario, placa);
        return ResponseEntity.ok(vehiculoService.reasignar(placa, datos.sucursalId()));
    }

    /** HU-14 (COMPANIA) */
    @PutMapping("/{placa}/estado")
    public ResponseEntity<Vehiculo> cambiarEstado(@PathVariable String placa,
                                                  @Valid @RequestBody Peticiones.CambioEstado datos,
                                                  @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        verificarVehiculoPropio(usuario, placa);
        return ResponseEntity.ok(vehiculoService.cambiarEstado(placa, datos.estado()));
    }

    private void verificarVehiculoPropio(Usuario usuario, String placa) {
        Vehiculo vehiculo = vehiculoService.consultarVehiculo(placa);
        if (!vehiculo.getCompaniaNit().equals(((UsuarioCompania) usuario).getCompaniaNit())) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
    }
}
