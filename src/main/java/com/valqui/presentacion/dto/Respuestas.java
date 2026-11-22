package com.valqui.presentacion.dto;

import com.valqui.modelo.EstadoReserva;
import com.valqui.modelo.PenalizacionDano;
import com.valqui.modelo.Reserva;
import com.valqui.modelo.Usuario;
import com.valqui.modelo.Vehiculo;

import java.time.LocalDateTime;
import java.util.List;

/** Cuerpos de las respuestas (DTOs de salida). */
public final class Respuestas {

    private Respuestas() {
    }

    /** Formato único de error. */
    public record Error(String mensaje, int codigo) {
    }

    public record Mensaje(String mensaje) {
    }

    public record UsuarioRegistrado(String id, String nombreCompleto, String correo, String tipo) {
        public static UsuarioRegistrado de(Usuario u) {
            return new UsuarioRegistrado(u.getId(), u.getNombreCompleto(), u.getCorreo(), u.getTipo());
        }
    }

    public record Sesion(String token, String tipo, String id) {
    }

    public record MetodoPagoRegistrado(String marca, String tipo, String ultimosDigitos, String mensaje) {
    }

    public record CompaniaRegistrada(String nit, String nombre, List<String> ciudadesOperacion,
                                     String correoAcceso, String contrasenaTemporal, String mensaje) {
    }

    public record Busqueda(List<Vehiculo> vehiculos, String mensaje) {
    }

    public record ReservaDetalle(String codigo, EstadoReserva estado, String clienteId, String placa,
                                 LocalDateTime fechaHoraRecogida, LocalDateTime fechaHoraDevolucion,
                                 String sucursalRecogidaId, String sucursalDevolucionId,
                                 double tarifaDiaria, Integer kilometrajeIncluido, double franquiciaDanos,
                                 double valorDeposito, List<String> coberturaIds, String metodoPago,
                                 PenalizacionDano penalizacionDano, double total) {
        public static ReservaDetalle de(Reserva r) {
            return new ReservaDetalle(r.getCodigo(), r.getEstado(), r.getClienteId(), r.getVehiculoPlaca(),
                    r.getFechaHoraRecogida(), r.getFechaHoraDevolucion(), r.getSucursalRecogidaId(),
                    r.getSucursalDevolucionId(), r.getTarifaDiaria(), r.getKilometrajeIncluido(),
                    r.getFranquiciaDanos(), r.getValorDeposito(), r.getCoberturaIds(), r.getMetodoPago(),
                    r.getPenalizacionDano(), r.calcularTotal());
        }
    }

    public record Cancelacion(String codigo, EstadoReserva estado, double penalizacion) {
    }
}
