package com.valqui.negocio;

import com.valqui.datos.ReservaRepository;
import com.valqui.datos.VehiculoRepository;
import com.valqui.modelo.EstadoVehiculo;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Reserva;
import com.valqui.modelo.Sucursal;
import com.valqui.modelo.Vehiculo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final ReservaRepository reservaRepository;
    private final CompaniaService companiaService;

    public VehiculoService(VehiculoRepository vehiculoRepository, ReservaRepository reservaRepository,
                           CompaniaService companiaService) {
        this.vehiculoRepository = vehiculoRepository;
        this.reservaRepository = reservaRepository;
        this.companiaService = companiaService;
    }

    /** HU-10: el vehículo queda DISPONIBLE en una sucursal de su compañía. */
    public Vehiculo registrarVehiculo(Vehiculo vehiculo) {
        vehiculo.setPlaca(vehiculo.getPlaca().trim().toUpperCase());
        if (vehiculoRepository.findByPlaca(vehiculo.getPlaca()).isPresent()) {
            throw new NegocioException(409, "Ya existe un vehículo registrado con esa placa");
        }
        Sucursal sucursal = companiaService.consultarSucursal(vehiculo.getCompaniaNit(), vehiculo.getSucursal());
        vehiculo.reasignar(sucursal);
        vehiculo.cambiarEstado(EstadoVehiculo.DISPONIBLE);
        try {
            return vehiculoRepository.insert(vehiculo);
        } catch (DuplicateKeyException e) {
            throw new NegocioException(409, "Ya existe un vehículo registrado con esa placa");
        }
    }

    /** HU-02: vehículos DISPONIBLE o RESERVADO sin reserva activa que se cruce con las fechas. */
    public List<Vehiculo> buscarDisponibles(String ciudad, LocalDateTime desde, LocalDateTime hasta,
                                            String categoria, String nit) {
        if (ciudad == null || ciudad.isBlank() || desde == null || hasta == null) {
            throw new NegocioException(400, "La ciudad y las fechas de recogida y devolución son obligatorias");
        }
        if (!hasta.isAfter(desde)) {
            throw new NegocioException(400, "La fecha de devolución debe ser posterior a la de recogida");
        }
        return vehiculoRepository.buscarPorFiltros(ciudad, categoria, nit).stream()
                .map(this::cargarReservasActivas)
                .filter(v -> v.estaDisponible(desde, hasta))
                .toList();
    }

    /** Devuelve el vehículo con sus reservas activas y su compañía dueña. */
    public Vehiculo consultarVehiculo(String placa) {
        Vehiculo vehiculo = vehiculoRepository.findByPlaca(placa == null ? null : placa.trim().toUpperCase())
                .map(this::cargarReservasActivas)
                .orElseThrow(() -> new NegocioException(404, "No se encontró el vehículo"));
        vehiculo.setCompania(companiaService.consultarCompania(vehiculo.getCompaniaNit()));
        return vehiculo;
    }

    /** HU-11: solo a una sucursal de la misma compañía y sin reserva activa. */
    public void reasignar(String placa, String sucursal) {
        Vehiculo vehiculo = consultarVehiculo(placa);
        Sucursal destino = companiaService.consultarSucursal(vehiculo.getCompaniaNit(), sucursal);
        if (vehiculo.tieneReservaActiva()) {
            throw new NegocioException(409, "No se puede reasignar un vehículo con una reserva activa");
        }
        vehiculo.reasignar(destino);
        vehiculoRepository.save(vehiculo);
    }

    /**
     * HU-14 y HU-16. RESERVADO solo lo pide EntregaService (con la reserva ya EN_CURSO);
     * VehiculoController rechaza que se asigne a mano.
     */
    public void cambiarEstado(String placa, EstadoVehiculo estado) {
        if (estado == null) {
            throw new NegocioException(400, "El estado es obligatorio");
        }
        Vehiculo vehiculo = consultarVehiculo(placa);
        if (estado != EstadoVehiculo.RESERVADO && vehiculo.tieneReservaActiva()) {
            throw new NegocioException(409, "No se puede cambiar el estado de un vehículo con una reserva activa");
        }
        vehiculo.cambiarEstado(estado);
        vehiculoRepository.save(vehiculo);
    }

    /** HU-17: el vehículo vuelve a DISPONIBLE y queda asignado a la sucursal de devolución. */
    public void registrarDevolucion(String placa, String sucursalDevolucion) {
        Vehiculo vehiculo = consultarVehiculo(placa);
        Sucursal sucursal = companiaService.consultarSucursal(vehiculo.getCompaniaNit(), sucursalDevolucion);
        vehiculo.reasignar(sucursal);
        vehiculo.cambiarEstado(EstadoVehiculo.DISPONIBLE);
        vehiculoRepository.save(vehiculo);
    }

    private Vehiculo cargarReservasActivas(Vehiculo vehiculo) {
        Reserva filtro = new Reserva();
        filtro.setVehiculoPlaca(vehiculo.getPlaca());
        ExampleMatcher criterio = ExampleMatcher.matching().withIgnorePaths("estado", "tarifaDiaria",
                "franquiciaDanos", "valorDeposito", "coberturaNombres");
        vehiculo.setReservasActivas(reservaRepository.findAll(Example.of(filtro, criterio)).stream()
                .filter(Reserva::estaActiva)
                .toList());
        return vehiculo;
    }
}
