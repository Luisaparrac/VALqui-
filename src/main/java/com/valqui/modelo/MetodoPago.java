package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

/**
 * Método de pago embebido en el Cliente. Nunca guarda el número completo de la tarjeta.
 */
public class MetodoPago {

    private static final Set<String> MARCAS_VALIDAS = Set.of("VISA", "MASTERCARD", "AMERICAN_EXPRESS");

    private String marca;
    private String tipo;
    private String ultimosDigitos;
    @JsonIgnore
    private String token;

    public MetodoPago() {
    }

    public MetodoPago(String marca, String tipo, String ultimosDigitos, String token) {
        this.marca = marca;
        this.tipo = tipo;
        this.ultimosDigitos = ultimosDigitos;
        this.token = token;
    }

    public boolean esMarcaValida() {
        return marca != null && MARCAS_VALIDAS.contains(marca.trim().toUpperCase());
    }

    public String getMarca() { return marca; }
    public String getTipo() { return tipo; }
    public String getUltimosDigitos() { return ultimosDigitos; }
    public String getToken() { return token; }
}
