package com.valqui.integracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cliente de la pasarela de pago. La pasarela es externa y no se construye:
 * esta implementación funciona como simulador.
 * Aprueba si el token no está vacío y el monto es mayor que 0; rechaza el token "tok_rechazado".
 */
@Component
public class PasarelaPagoClient implements IPasarelaPago {

    private static final Logger log = LoggerFactory.getLogger(PasarelaPagoClient.class);
    static final String TOKEN_RECHAZADO = "tok_rechazado";

    private final String urlApi;
    private final String apiKey;

    public PasarelaPagoClient(@Value("${pasarela.url-api}") String urlApi,
                              @Value("${pasarela.api-key}") String apiKey) {
        this.urlApi = urlApi;
        this.apiKey = apiKey;
    }

    @Override
    public boolean procesarCobro(String token, double monto) {
        log.info("Cobro simulado contra {} por {}", urlApi, monto);
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        if (token == null || token.isBlank() || TOKEN_RECHAZADO.equals(token)) {
            return false;
        }
        return monto > 0;
    }
}
