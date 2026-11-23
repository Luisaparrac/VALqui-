package com.valqui.negocio;

import com.valqui.datos.CompaniaRepository;
import com.valqui.modelo.Cobertura;
import com.valqui.modelo.CompaniaAliada;
import com.valqui.modelo.NegocioException;
import com.valqui.modelo.Sucursal;
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

    /** HU-15: registra la compañía y crea su cuenta de acceso. */
    public CompaniaAliada registrarCompania(CompaniaAliada compania, String correoAcceso) {
        if (companiaRepository.findByNit(compania.getNit()).isPresent()) {
            throw new NegocioException(409, "Ya existe una compañía registrada con ese NIT");
        }
        try {
            companiaRepository.insert(compania);
        } catch (DuplicateKeyException e) {
            throw new NegocioException(409, "Ya existe una compañía registrada con ese NIT");
        }
        try {
            usuarioService.crearCuentaCompania(compania.getNit(), correoAcceso);
        } catch (NegocioException e) {
            companiaRepository.delete(compania);
            throw e;
        }
        return compania;
    }

    /** HU-09 */
    public void agregarSucursal(String nit, Sucursal sucursal) {
        CompaniaAliada compania = consultarCompania(nit);
        compania.agregarSucursal(sucursal);
        companiaRepository.save(compania);
    }

    /** HU-12 */
    public void configurarPolitica(String nit, int horasGratis, double penalizacion, double deposito) {
        CompaniaAliada compania = consultarCompania(nit);
        compania.setHorasCancelacionGratuita(horasGratis);
        compania.setPenalizacionCancelacion(penalizacion);
        compania.setValorDeposito(deposito);
        companiaRepository.save(compania);
    }

    /** HU-13 */
    public void agregarCobertura(String nit, Cobertura cobertura) {
        CompaniaAliada compania = consultarCompania(nit);
        compania.agregarCobertura(cobertura);
        companiaRepository.save(compania);
    }

    public CompaniaAliada consultarCompania(String nit) {
        return companiaRepository.findByNit(nit)
                .orElseThrow(() -> new NegocioException(404, "No se encontró la compañía"));
    }

    /** Devuelve la sucursal solo si pertenece a la compañía indicada. */
    public Sucursal consultarSucursal(String nit, String nombre) {
        String buscado = nombre == null ? "" : nombre.trim();
        return consultarCompania(nit).getSucursales().stream()
                .filter(s -> s.getNombre().equalsIgnoreCase(buscado))
                .findFirst()
                .orElseThrow(() -> new NegocioException(400, "La sucursal no pertenece a la compañía"));
    }

    public List<Cobertura> consultarCoberturas(String nit) {
        return consultarCompania(nit).getCoberturas();
    }
}
