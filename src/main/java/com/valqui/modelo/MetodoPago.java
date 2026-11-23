package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

/**
 * Método de pago embebido en el Cliente. Nunca guarda el número completo de la tarjeta.
 * Su token es el dato que PagoService entrega a IPasarelaPago.procesarCobro.
 */
public class MetodoPago {

    private static final Set<String> MARCAS_VALIDAS = Set.of("VISA", "MASTERCARD", "AMERICAN_EXPRESS");

    @NotBlank(message = "La marca es obligatoria")
    private String marca;
    @NotBlank(message = "El tipo es obligatorio")
    @Pattern(regexp = "CREDITO|DEBITO", message = "El tipo debe ser CREDITO o DEBITO")
    private String tipo;
    @NotBlank(message = "Los últimos dígitos son obligatorios")
    @Pattern(regexp = "\\d{4}", message = "Los últimos dígitos deben ser 4 números")
    private String ultimosDigitos;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "El token de la tarjeta es obligatorio")
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
