package com.valqui.datos;

import com.valqui.modelo.Vehiculo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends MongoRepository<Vehiculo, String> {

    Optional<Vehiculo> findByPlaca(String placa);

    List<Vehiculo> findBySucursalCiudadIgnoreCase(String ciudad);

    /** Vehículos asignados a sucursales de la ciudad; categoría y nit son filtros opcionales. */
    default List<Vehiculo> buscarPorFiltros(String ciudad, String categoria, String nit) {
        return findBySucursalCiudadIgnoreCase(ciudad.trim()).stream()
                .filter(v -> categoria == null || categoria.isBlank() || categoria.trim().equalsIgnoreCase(v.getCategoria()))
                .filter(v -> nit == null || nit.isBlank() || nit.trim().equals(v.getCompaniaNit()))
                .toList();
    }
}
