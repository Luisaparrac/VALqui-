package com.valqui.integracion;

/** Contrato con la pasarela de pago externa. */
public interface IPasarelaPago {

    boolean procesarCobro(String token, double monto);
}
