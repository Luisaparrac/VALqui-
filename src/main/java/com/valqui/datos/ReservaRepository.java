package com.valqui.datos;

import com.valqui.modelo.EstadoReserva;
import com.valqui.modelo.Reserva;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

    List<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    Optional<Reserva> findByCodigo(String codigo);

    List<Reserva> findByClienteId(String clienteId);

    List<Reserva> findByVehiculoPlacaAndEstadoIn(String placa, Collection<EstadoReserva> estados);

    /** Cruce de fechas: recogidaA < devolucionB && devolucionA > recogidaB, solo reservas activas. */
    @Query(value = "{ 'vehiculoPlaca': ?0, 'estado': { $in: ['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'] },"
            + " 'fechaHoraRecogida': { $lt: ?2 }, 'fechaHoraDevolucion': { $gt: ?1 } }", exists = true)
    boolean existeReservaActiva(String placa, LocalDateTime desde, LocalDateTime hasta);

    boolean existsByCodigo(String codigo);
}
