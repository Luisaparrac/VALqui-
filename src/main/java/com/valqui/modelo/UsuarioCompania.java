package com.valqui.modelo;

/** Usuario que representa a una compañía aliada (relación «representa a»). */
public class UsuarioCompania extends Usuario {

    private String cargo;
    private String telefono;
    private String companiaNit;

    public UsuarioCompania() {
        super(COMPANIA);
    }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCompaniaNit() { return companiaNit; }
    public void setCompaniaNit(String companiaNit) { this.companiaNit = companiaNit; }
}
