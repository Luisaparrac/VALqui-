package com.valqui.negocio;

import com.valqui.datos.ReservaRepository;
import com.valqui.datos.VehiculoRepository;
import com.valqui.modelo.CompaniaAliada;
import com.valqui.modelo.EstadoVehiculo;
import com.valqui.modelo.Sucursal;
import com.valqui.modelo.Vehiculo;
import org.springframework.dao.DuplicateKeyException;
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
    public Vehiculo registrarVehiculo(Vehiculo vehiculo, String sucursalId) {
        vehiculo.setPlaca(vehiculo.getPlaca().trim().toUpperCase());
        if (vehiculoRepository.findByPlaca(vehiculo.getPlaca()).isPresent()) {
            throw new NegocioException(409, "Ya existe un vehículo registrado con esa placa");
        }
        Sucursal sucursal = companiaService.consultarSucursal(vehiculo.getCompaniaNit(), sucursalId);
        vehiculo.reasignar(sucursal);
        vehiculo.cambiarEstado(EstadoVehiculo.DISPONIBLE);
        try {
            return vehiculoRepository.save(vehiculo);
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

    public Vehiculo consultarVehiculo(String placa) {
        return vehiculoRepository.findByPlaca(placa == null ? null : placa.trim().toUpperCase())
                .map(this::cargarReservasActivas)
                .orElseThrow(() -> new NegocioException(404, "No se encontró el vehículo"));
    }

    /** HU-03 / R15 */
    public DetalleVehiculo consultarDetalle(String placa) {
        Vehiculo vehiculo = consultarVehiculo(placa);
        CompaniaAliada compania = companiaService.consultarCompania(vehiculo.getCompaniaNit());
        return new DetalleVehiculo(vehiculo, compania.getNombre(), compania.getValorDeposito(),
                vehiculo.getFranquiciaDanos(), vehiculo.getKilometrajeIncluido(),
                vehiculo.getKilometrajeIncluido() == null,
                new DetalleVehiculo.PoliticaCancelacion(compania.getHorasCancelacionGratuita(),
                        compania.getPenalizacionCancelacion()),
                compania.getCoberturas());
    }

    /** HU-11: solo a una sucursal de la misma compañía y sin reserva activa. */
    public Vehiculo reasignar(String placa, String sucursalId) {
        Vehiculo vehiculo = consultarVehiculo(placa);
        Sucursal sucursal = companiaService.consultarSucursal(vehiculo.getCompaniaNit(), sucursalId);
        if (vehiculo.tieneReservaActiva()) {
            throw new NegocioException(409, "No se puede reasignar un vehículo con una reserva activa");
        }
        vehiculo.reasignar(sucursal);
        return vehiculoRepository.save(vehiculo);
    }

    /** HU-14: solo entre DISPONIBLE, EN_MANTENIMIENTO y FUERA_DE_SERVICIO. */
    public Vehiculo cambiarEstado(String placa, EstadoVehiculo estado) {
        if (estado == null) {
            throw new NegocioException(400, "El estado es obligatorio");
        }
        if (estado == EstadoVehiculo.RESERVADO) {
            throw new NegocioException(400, "El estado RESERVADO lo asigna el sistema, no se puede asignar manualmente");
        }
        Vehiculo vehiculo = consultarVehiculo(placa);
        if (vehiculo.tieneReservaActiva()) {
            throw new NegocioException(409, "No se puede cambiar el estado de un vehículo con una reserva activa");
        }
        vehiculo.cambiarEstado(estado);
        return vehiculoRepository.save(vehiculo);
    }

    /** HU-16: al entregar el vehículo queda RESERVADO (en uso). */
    public Vehiculo registrarEntrega(String placa) {
        Vehiculo vehiculo = consultarVehiculo(placa);
        vehiculo.cambiarEstado(EstadoVehiculo.RESERVADO);
        return vehiculoRepository.save(vehiculo);
    }

    /** HU-17: el vehículo vuelve a DISPONIBLE y queda asignado a la sucursal de devolución. */
    public Vehiculo registrarDevolucion(String placa, String sucursalDevolucion) {
        Vehiculo vehiculo = consultarVehiculo(placa);
        Sucursal sucursal = companiaService.consultarSucursal(vehiculo.getCompaniaNit(), sucursalDevolucion);
        vehiculo.reasignar(sucursal);
        vehiculo.cambiarEstado(EstadoVehiculo.DISPONIBLE);
        return vehiculoRepository.save(vehiculo);
    }

    private Vehiculo cargarReservasActivas(Vehiculo vehiculo) {
        vehiculo.setReservasActivas(reservaRepository.findByVehiculoPlacaAndEstadoIn(
                vehiculo.getPlaca(), ReservaRepository.ESTADOS_ACTIVOS));
        return vehiculo;
    }
}
