package com.valqui.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Usuario abstracto. Cliente, UsuarioCompania, AgenteSucursal y Administrador
 * se guardan en la misma colección "usuarios" y se distinguen por el campo "tipo".
 */
@Document(collection = "usuarios")
public abstract class Usuario {

    public static final String CLIENTE = "CLIENTE";
    public static final String COMPANIA = "COMPANIA";
    public static final String AGENTE = "AGENTE";
    public static final String ADMIN = "ADMIN";

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    @Id
    private String id;
    private String nombreCompleto;
    @Indexed(unique = true)
    private String correo;
    @JsonIgnore
    private String contrasena;
    private String tipo;

    protected Usuario(String tipo) {
        this.tipo = tipo;
    }

    public boolean iniciarSesion(String correo, String contrasena) {
        return this.correo != null && this.correo.equalsIgnoreCase(correo)
                && contrasena != null && this.contrasena != null
                && BCRYPT.matches(contrasena, this.contrasena);
    }

    /** Guarda la contraseña cifrada con BCrypt. */
    public void setContrasena(String contrasena) {
        this.contrasena = BCRYPT.encode(contrasena);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTipo() { return tipo; }
}
