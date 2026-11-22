package com.valqui.negocio;

/**
 * Excepción de negocio que lleva el código HTTP con el que se debe responder.
 */
public class NegocioException extends RuntimeException {

    private final int codigo;

    public NegocioException(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
