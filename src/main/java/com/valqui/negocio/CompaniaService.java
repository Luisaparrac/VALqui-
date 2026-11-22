package com.valqui.negocio;

import com.valqui.datos.CompaniaRepository;
import com.valqui.modelo.Cobertura;
import com.valqui.modelo.CompaniaAliada;
import com.valqui.modelo.Sucursal;
import com.valqui.modelo.UsuarioCompania;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompaniaService {

    private final CompaniaRepository companiaRepository;
    private final UsuarioService usuarioService;

    public CompaniaService(CompaniaRepository companiaRepository, UsuarioService usuarioService) {
        this.companiaRepository = companiaRepository;
        this.usuarioService = usuarioService;
    }

    /** HU-15: registra la compañía y crea su cuenta de acceso. Devuelve la contraseña temporal. */
    public String registrarCompania(CompaniaAliada compania, String correoAcceso, UsuarioCompania representante) {
        if (companiaRepository.findByNit(compania.getNit()).isPresent()) {
            throw new NegocioException(409, "Ya existe una compañía registrada con ese NIT");
        }
        try {
            companiaRepository.save(compania);
        } catch (DuplicateKeyException e) {
            throw new NegocioException(409, "Ya existe una compañía registrada con ese NIT");
        }
        try {
            return usuarioService.crearCuentaCompania(compania.getNit(), correoAcceso, representante);
        } catch (NegocioException e) {
            companiaRepository.delete(compania);
            throw e;
        }
    }

    /** HU-09 */
    public Sucursal agregarSucursal(String nit, Sucursal sucursal) {
        CompaniaAliada compania = consultarCompania(nit);
        compania.agregarSucursal(sucursal);
        companiaRepository.save(compania);
        return sucursal;
    }

    /** HU-12 */
    public CompaniaAliada configurarPolitica(String nit, int horasGratis, double penalizacion, double deposito) {
        CompaniaAliada compania = consultarCompania(nit);
        compania.setHorasCancelacionGratuita(horasGratis);
        compania.setPenalizacionCancelacion(penalizacion);
        compania.setValorDeposito(deposito);
        return companiaRepository.save(compania);
    }

    /** HU-13 */
    public Cobertura agregarCobertura(String nit, Cobertura cobertura) {
        CompaniaAliada compania = consultarCompania(nit);
        compania.agregarCobertura(cobertura);
        companiaRepository.save(compania);
        return cobertura;
    }

    public CompaniaAliada consultarCompania(String nit) {
        return companiaRepository.findByNit(nit)
                .orElseThrow(() -> new NegocioException(404, "No se encontró la compañía"));
    }

    /** Devuelve la sucursal solo si pertenece a la compañía indicada. */
    public Sucursal consultarSucursal(String nit, String sucursalId) {
        return consultarCompania(nit).getSucursales().stream()
                .filter(s -> s.getId().equals(sucursalId))
                .findFirst()
                .orElseThrow(() -> new NegocioException(400, "La sucursal no pertenece a la compañía"));
    }

    public List<Cobertura> consultarCoberturas(String nit) {
        return consultarCompania(nit).getCoberturas();
    }
}
