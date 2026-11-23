package com.valqui.datos;

import com.valqui.modelo.Sucursal;
import com.valqui.modelo.Vehiculo;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends MongoRepository<Vehiculo, String> {

    Optional<Vehiculo> findByPlaca(String placa);

    /** Vehículos asignados a sucursales de la ciudad; categoría y nit son filtros opcionales. */
    default List<Vehiculo> buscarPorFiltros(String ciudad, String categoria, String nit) {
        Sucursal ubicacion = new Sucursal();
        ubicacion.setCiudad(ciudad.trim());
        Vehiculo filtro = new Vehiculo();
        filtro.reasignar(ubicacion);
        filtro.setCategoria(categoria == null || categoria.isBlank() ? null : categoria.trim());
        filtro.setCompaniaNit(nit == null || nit.isBlank() ? null : nit.trim());
        ExampleMatcher criterio = ExampleMatcher.matching()
                .withIgnorePaths("anio", "capacidadPasajeros", "tarifaDiaria", "franquiciaDanos")
                .withIgnoreCase("sucursalCiudad", "categoria");
        return findAll(Example.of(filtro, criterio));
    }
}
