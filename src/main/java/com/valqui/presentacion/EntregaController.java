package com.valqui.presentacion;

import com.valqui.modelo.AgenteSucursal;
import com.valqui.modelo.Usuario;
import com.valqui.negocio.EntregaService;
import com.valqui.negocio.ResultadoEntrega;
import com.valqui.presentacion.dto.Peticiones;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entregas")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    /** HU-16 (AGENTE) */
    @PostMapping("/{codigo}/entrega")
    public ResponseEntity<ResultadoEntrega> entregarVehiculo(@PathVariable String codigo,
                                                             @Valid @RequestBody Peticiones.Entrega datos,
                                                             @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        return ResponseEntity.ok(entregaService.entregar((AgenteSucursal) usuario, codigo, datos.documentosValidos()));
    }

    /** HU-17 (AGENTE) */
    @PostMapping("/{codigo}/devolucion")
    public ResponseEntity<ResultadoEntrega> recibirVehiculo(@PathVariable String codigo,
                                                            @Valid @RequestBody(required = false) Peticiones.Devolucion datos,
                                                            @RequestAttribute(FiltroSesion.ATRIBUTO_USUARIO) Usuario usuario) {
        Double valorDano = datos == null ? null : datos.valorDano();
        String motivoDano = datos == null ? null : datos.motivoDano();
        return ResponseEntity.ok(entregaService.recibir((AgenteSucursal) usuario, codigo, valorDano, motivoDano));
    }
}
