package com.valqui.modelo;

import com.valqui.negocio.NegocioException;

public class Administrador extends Usuario {

    private String codigoAdministrador;

    public Administrador() {
        super(ADMIN);
    }

    /** Valida los datos mínimos de la compañía antes de registrarla en la plataforma. */
    public void registrarCompania(CompaniaAliada compania) {
        if (esVacio(compania.getNit()) || esVacio(compania.getNombre())
                || compania.getCiudadesOperacion() == null || compania.getCiudadesOperacion().isEmpty()
                || compania.getCiudadesOperacion().stream().anyMatch(Administrador::esVacio)) {
            throw new NegocioException(400, "Datos de la compañía inválidos: nit, nombre y al menos una ciudad de operación son obligatorios");
        }
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    public String getCodigoAdministrador() { return codigoAdministrador; }
    public void setCodigoAdministrador(String codigoAdministrador) { this.codigoAdministrador = codigoAdministrador; }
}
