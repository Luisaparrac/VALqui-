package com.valqui.datos;

import com.valqui.modelo.CompaniaAliada;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CompaniaRepository extends MongoRepository<CompaniaAliada, String> {

    Optional<CompaniaAliada> findByNit(String nit);
}
