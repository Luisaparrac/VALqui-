package com.valqui.presentacion;

import com.valqui.modelo.Administrador;
import com.valqui.modelo.Cobertura;
import com.valqui.modelo.CompaniaAliada;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Sucursal;
import com.valqui.modelo.Usuario;
import com.valqui.modelo.UsuarioCompania;
import com.valqui.negocio.CompaniaService;
import com.valqui.presentacion.dto.Respuestas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@RestController
@RequestMapping("/api/companias")
public class CompaniaController {

    private final CompaniaService companiaService;

    public CompaniaController(CompaniaService companiaService) {
        this.companiaService = companiaService;
    }

    /** HU-15 (ADMIN): la contraseña inicial de la cuenta es el NIT. */
    @PostMapping
    public ResponseEntity<Respuestas.CompaniaRegistrada> registrarCompania(
            @Valid @RequestBody CompaniaAliada compania,
            @RequestParam @NotBlank(message = "El correo de acceso es obligatorio")
            @Email(message = "El correo de acceso no es válido") String correoAcceso) {
        compania.setNit(compania.getNit().trim());
        compania.setNombre(compania.getNombre().trim());
        compania.setCiudadesOperacion(compania.getCiudadesOperacion().stream().map(String::trim).toList());
        ((Administrador) usuarioSesion()).registrarCompania(compania);

        CompaniaAliada registrada = companiaService.registrarCompania(compania, correoAcceso);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Respuestas.CompaniaRegistrada(
                registrada.getNit(), registrada.getNombre(), registrada.getCiudadesOperacion(),
                correoAcceso.trim().toLowerCase(),
                "Compañía registrada. La contraseña inicial de la cuenta es el NIT"));
    }

    /** HU-09 */
    @PostMapping("/{nit}/sucursales")
    public ResponseEntity<Sucursal> registrarSucursal(@PathVariable String nit,
                                                      @Valid @RequestBody Sucursal sucursal) {
        verificarCompaniaPropia(nit);
        sucursal.setNombre(sucursal.getNombre().trim());
        sucursal.setCiudad(sucursal.getCiudad().trim());
        companiaService.agregarSucursal(nit, sucursal);
        return ResponseEntity.status(HttpStatus.CREATED).body(sucursal);
    }

    /** HU-12 */
    @PutMapping("/{nit}/politica")
    public ResponseEntity<Respuestas.Politica> configurarPolitica(
            @PathVariable String nit,
            @RequestParam @Min(value = 0, message = "Las horas de cancelación gratuita no pueden ser negativas") int horasGratis,
            @RequestParam @PositiveOrZero(message = "La penalización no puede ser negativa") double penalizacion,
            @RequestParam @PositiveOrZero(message = "El depósito no puede ser negativo") double deposito) {
        verificarCompaniaPropia(nit);
        companiaService.configurarPolitica(nit, horasGratis, penalizacion, deposito);
        return ResponseEntity.ok(new Respuestas.Politica(horasGratis, penalizacion, deposito));
    }

    /** HU-13 */
    @PostMapping("/{nit}/coberturas")
    public ResponseEntity<Cobertura> agregarCobertura(@PathVariable String nit,
                                                      @Valid @RequestBody Cobertura cobertura) {
        verificarCompaniaPropia(nit);
        cobertura.setNombre(cobertura.getNombre().trim());
        companiaService.agregarCobertura(nit, cobertura);
        return ResponseEntity.status(HttpStatus.CREATED).body(cobertura);
    }

    private static void verificarCompaniaPropia(String nit) {
        if (!(usuarioSesion() instanceof UsuarioCompania uc) || !nit.equals(uc.getCompaniaNit())) {
            throw new NegocioException(403, "No tienes permiso para realizar esta acción");
        }
    }

    private static Usuario usuarioSesion() {
        return (Usuario) RequestContextHolder.currentRequestAttributes()
                .getAttribute(FiltroSesion.ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST);
    }
}
