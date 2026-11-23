package com.valqui;

import com.valqui.datos.CompaniaRepository;
import com.valqui.datos.UsuarioRepository;
import com.valqui.modelo.Administrador;
import com.valqui.modelo.AgenteSucursal;
import com.valqui.modelo.CompaniaAliada;
import com.valqui.modelo.Sucursal;
import com.valqui.modelo.UsuarioCompania;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Datos semilla: solo se crean si la base de datos está vacía. */
@Component
public class CargaInicial implements CommandLineRunner {

    public static final String NIT_RENTACOL = "900123456-7";
    public static final String SUCURSAL_NORTE = "Sucursal Norte";
    public static final String SUCURSAL_SUR = "Sucursal Sur";

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private final UsuarioRepository usuarioRepository;
    private final CompaniaRepository companiaRepository;

    public CargaInicial(UsuarioRepository usuarioRepository, CompaniaRepository companiaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.companiaRepository = companiaRepository;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0 || companiaRepository.count() > 0) {
            log.info("Carga inicial omitida: la base de datos ya tiene datos");
            return;
        }

        Administrador admin = new Administrador();
        admin.setNombreCompleto("Administrador VAlqui");
        admin.setCorreo("admin@valqui.com");
        admin.setContrasena(BCRYPT.encode("Admin123*"));
        admin.setCodigoAdministrador("ADM-001");
        usuarioRepository.save(admin);

        CompaniaAliada rentaCol = new CompaniaAliada();
        rentaCol.setNit(NIT_RENTACOL);
        rentaCol.setNombre("RentaCol");
        rentaCol.setCiudadesOperacion(List.of("Bogotá", "Medellín"));
        rentaCol.setHorasCancelacionGratuita(24);
        rentaCol.setPenalizacionCancelacion(50000);
        rentaCol.setValorDeposito(300000);
        rentaCol.agregarSucursal(new Sucursal(SUCURSAL_NORTE, "Bogotá", "Calle 140 # 15-20", null, "Lunes a domingo 6:00 - 22:00"));
        rentaCol.agregarSucursal(new Sucursal(SUCURSAL_SUR, "Bogotá", "Autopista Sur # 60-10", null, "Lunes a sábado 7:00 - 20:00"));
        companiaRepository.save(rentaCol);

        UsuarioCompania usuarioRentaCol = new UsuarioCompania();
        usuarioRentaCol.setNombreCompleto("Representante RentaCol");
        usuarioRentaCol.setCorreo("rentacol@valqui.com");
        usuarioRentaCol.setContrasena(BCRYPT.encode("Renta123*"));
        usuarioRentaCol.setCargo("Gerente");
        usuarioRentaCol.setTelefono("6011234567");
        usuarioRentaCol.setCompaniaNit(NIT_RENTACOL);
        usuarioRepository.save(usuarioRentaCol);

        AgenteSucursal agente = new AgenteSucursal();
        agente.setNombreCompleto("Agente Sucursal Norte");
        agente.setCorreo("agente.norte@valqui.com");
        agente.setContrasena(BCRYPT.encode("Agente123*"));
        agente.setCodigoEmpleado("AGT-001");
        agente.setSucursal(SUCURSAL_NORTE);
        agente.setCompaniaNit(NIT_RENTACOL);
        usuarioRepository.save(agente);

        log.info("Carga inicial completada: administrador, compañía RentaCol, usuario de RentaCol y agente de Sucursal Norte");
    }
}
