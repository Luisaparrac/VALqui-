package com.valqui.presentacion;

import com.valqui.modelo.Administrador;
import com.valqui.modelo.Cobertura;
import com.valqui.modelo.CompaniaAliada;
import com.valqui.modelo.Sucursal;
import com.valqui.modelo.Usuario;
import com.valqui.modelo.UsuarioCompania;
import com.valqui.negocio.CompaniaService;
import com.valqui.negocio.NegocioException;
import com.valqui.presentacion.dto.Peticiones;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companias")
public class CompaniaController {

    private final CompaniaService companiaService;

    public CompaniaController(CompaniaService companiaService) {
        this.companiaService = companiaService;
    }

    /** HU-15 (ADMIN): devuelve la contraseña temporal una sola vez. */
    @PostMapping
    public ResponseEntity<Respuestas.CompaniaRegistrada> registrarCompania(
            @Valid @RequestBody Peticiones.RegistroCompania datos,
            @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        CompaniaAliada compania = new CompaniaAliada();
        compania.setNit(datos.nit().trim());
        compania.setNombre(datos.nombre().trim());
        compania.setCiudadesOperacion(datos.ciudadesOperacion().stream().map(String::trim).toList());
        ((Administrador) usuario).registrarCompania(compania);

        UsuarioCompania representante = new UsuarioCompania();
        representante.setNombreCompleto(datos.nombreCompleto().trim());
        representante.setCargo(datos.cargo());
        representante.setTelefono(datos.telefono());

        String contrasenaTemporal = companiaService.registrarCompania(compania, datos.correoAcceso(), representante);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Respuestas.CompaniaRegistrada(
                compania.getNit(), compania.getNombre(), compania.getCiudadesOperacion(),
                datos.correoAcceso().trim().toLowerCase(), contrasenaTemporal,
                "Compañía registrada. Guarda la contraseña temporal: solo se muestra una vez"));
    }

    /** HU-09 */
    @PostMapping("/{nit}/sucursales")
    public ResponseEntity<Sucursal> registrarSucursal(@PathVariable String nit,
                                                      @Valid @RequestBody Peticiones.RegistroSucursal sucursal,
                                                      @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        verificarCompaniaPropia(usuario, nit);
        Sucursal nueva = new Sucursal(sucursal.nombre().trim(), sucursal.ciudad().trim(), sucursal.direccion(),
                sucursal.coordenadas(), sucursal.horarioAtencion());
        return ResponseEntity.status(HttpStatus.CREATED).body(companiaService.agregarSucursal(nit, nueva));
    }

    /** HU-12 */
    @PutMapping("/{nit}/politica")
    public ResponseEntity<Peticiones.Politica> configurarPolitica(@PathVariable String nit,
                                                                  @Valid @RequestBody Peticiones.Politica politica,
                                                                  @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        verificarCompaniaPropia(usuario, nit);
        CompaniaAliada compania = companiaService.configurarPolitica(nit, politica.horasCancelacionGratuita(),
                politica.penalizacionCancelacion(), politica.valorDeposito());
        return ResponseEntity.ok(new Peticiones.Politica(compania.getHorasCancelacionGratuita(),
                compania.getPenalizacionCancelacion(), compania.getValorDeposito()));
    }

    /** HU-13 */
    @PostMapping("/{nit}/coberturas")
    public ResponseEntity<Cobertura> agregarCobertura(@PathVariable String nit,
                                                      @Valid @RequestBody Peticiones.RegistroCobertura cobertura,
                                                      @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        verificarCompaniaPropia(usuario, nit);
        Cobertura nueva = new Cobertura(cobertura.nombre().trim(), cobertura.costo());
        return ResponseEntity.status(HttpStatus.CREATED).body(companiaService.agregarCobertura(nit, nueva));
    }

    private static void verificarCompaniaPropia(Usuario usuario, String nit) {
        if (!(usuario instanceof UsuarioCompania uc) || !nit.equals(uc.getCompaniaNit())) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
    }
}
