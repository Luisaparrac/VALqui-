package com.valqui.modelo;

/** Penalización por daño embebida en la reserva (composición «genera», 0..1). */
public class PenalizacionDano {

    private double valor;
    private String motivo;

    public PenalizacionDano() {
    }

    public PenalizacionDano(double valor, String motivo) {
        this.valor = valor;
        this.motivo = motivo;
    }

    public double getValor() { return valor; }
    public String getMotivo() { return motivo; }
}
