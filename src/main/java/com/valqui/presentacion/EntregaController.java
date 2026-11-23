package com.valqui.presentacion;

import com.valqui.modelo.EstadoReserva;
import com.valqui.modelo.EstadoVehiculo;
import com.valqui.modelo.PenalizacionDano;
import com.valqui.negocio.EntregaService;
import com.valqui.presentacion.dto.ResultadoEntrega;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
                                                             @RequestParam boolean documentosValidos) {
        entregaService.entregar(codigo, documentosValidos);
        return ResponseEntity.ok(new ResultadoEntrega(codigo, EstadoReserva.EN_CURSO, EstadoVehiculo.RESERVADO, null));
    }

    /** HU-17 (AGENTE): valorDano = 0 significa sin daño. */
    @PostMapping("/{codigo}/devolucion")
    public ResponseEntity<ResultadoEntrega> recibirVehiculo(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "0")
            @PositiveOrZero(message = "El valor del daño debe ser mayor que 0") double valorDano,
            @RequestParam(required = false) String motivoDano) {
        entregaService.recibir(codigo, valorDano, motivoDano);
        PenalizacionDano penalizacion = valorDano > 0 ? new PenalizacionDano(valorDano, motivoDano) : null;
        return ResponseEntity.ok(new ResultadoEntrega(codigo, EstadoReserva.FINALIZADA, EstadoVehiculo.DISPONIBLE, penalizacion));
    }
}
