package com.valqui.negocio;

import com.valqui.integracion.IPasarelaPago;
import com.valqui.modelo.MetodoPago;
import org.springframework.stereotype.Service;

@Service
public class PagoService {

    private final IPasarelaPago pasarelaPago;
    private final UsuarioService usuarioService;

    public PagoService(IPasarelaPago pasarelaPago, UsuarioService usuarioService) {
        this.pasarelaPago = pasarelaPago;
        this.usuarioService = usuarioService;
    }

    public boolean cobrar(String clienteId, String ultimosDigitos, double monto) {
        MetodoPago metodoPago = usuarioService.obtenerMetodoPago(clienteId, ultimosDigitos);
        return pasarelaPago.procesarCobro(metodoPago.getToken(), monto);
    }
}
