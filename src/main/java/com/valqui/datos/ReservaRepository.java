package com.valqui.datos;

import com.valqui.modelo.Reserva;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

    List<Reserva> findByClienteId(String clienteId);

    /**
     * Cruce de fechas: recogidaA < devolucionB && devolucionA > recogidaB, solo reservas activas.
     * La @Query necesita un texto constante: los estados deben coincidir con Reserva.estaActiva().
     */
    @Query(value = "{ 'vehiculoPlaca': ?0, 'estado': { $in: ['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'] },"
            + " 'fechaHoraRecogida': { $lt: ?2 }, 'fechaHoraDevolucion': { $gt: ?1 } }", exists = true)
    boolean existeReservaActiva(String placa, LocalDateTime desde, LocalDateTime hasta);
}
