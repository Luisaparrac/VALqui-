package com.valqui.presentacion.dto;

import com.valqui.modelo.EstadoVehiculo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Cuerpos de las peticiones (DTOs de entrada). */
public final class Peticiones {

    private Peticiones() {
    }

    /** HU-01 */
    public record RegistroCliente(
            @NotBlank(message = "El nombre completo es obligatorio") String nombreCompleto,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo,
            @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String contrasena,
            @NotNull(message = "La fecha de nacimiento es obligatoria") @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
            @NotBlank(message = "El número de contacto es obligatorio") String numeroContacto,
            @NotBlank(message = "El país de residencia es obligatorio") String paisResidencia) {
    }

    /** HU-18 */
    public record Login(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String contrasena) {
    }

    /** R03: la tarjeta llega tokenizada; nunca se recibe ni se guarda el número completo. */
    public record RegistroMetodoPago(
            @NotBlank(message = "La marca es obligatoria") String marca,
            @NotBlank(message = "El tipo es obligatorio") @Pattern(regexp = "CREDITO|DEBITO", message = "El tipo debe ser CREDITO o DEBITO") String tipo,
            @NotBlank(message = "Los últimos dígitos son obligatorios") @Pattern(regexp = "\\d{4}", message = "Los últimos dígitos deben ser 4 números") String ultimosDigitos,
            @NotBlank(message = "El token de la tarjeta es obligatorio") String token) {
    }

    /** HU-15 */
    public record RegistroCompania(
            @NotBlank(message = "El NIT es obligatorio") String nit,
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotEmpty(message = "Debe indicar al menos una ciudad de operación") List<@NotBlank String> ciudadesOperacion,
            @NotBlank(message = "El correo de acceso es obligatorio") @Email(message = "El correo de acceso no es válido") String correoAcceso,
            @NotBlank(message = "El nombre del representante es obligatorio") String nombreCompleto,
            @NotBlank(message = "El cargo del representante es obligatorio") String cargo,
            @NotBlank(message = "El teléfono del representante es obligatorio") String telefono) {
    }

    /** HU-09 */
    public record RegistroSucursal(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "La ciudad es obligatoria") String ciudad,
            @NotBlank(message = "La dirección es obligatoria") String direccion,
            String coordenadas,
            @NotBlank(message = "El horario de atención es obligatorio") String horarioAtencion) {
    }

    /** HU-12 */
    public record Politica(
            @NotNull(message = "Las horas de cancelación gratuita son obligatorias") @Min(value = 0, message = "Las horas de cancelación gratuita no pueden ser negativas") Integer horasCancelacionGratuita,
            @NotNull(message = "La penalización de cancelación es obligatoria") @PositiveOrZero(message = "La penalización no puede ser negativa") Double penalizacionCancelacion,
            @NotNull(message = "El valor del depósito es obligatorio") @PositiveOrZero(message = "El depósito no puede ser negativo") Double valorDeposito) {
    }

    /** HU-13 */
    public record RegistroCobertura(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotNull(message = "El costo es obligatorio") @PositiveOrZero(message = "El costo no puede ser negativo") Double costo) {
    }

    /** HU-10 */
    public record RegistroVehiculo(
            @NotBlank(message = "La placa es obligatoria") String placa,
            @NotBlank(message = "La marca es obligatoria") String marca,
            @NotBlank(message = "El modelo es obligatorio") String modelo,
            @NotBlank(message = "La categoría es obligatoria") String categoria,
            @NotNull(message = "El año es obligatorio") @Min(value = 1900, message = "El año no es válido") Integer anio,
            @NotBlank(message = "La transmisión es obligatoria") String transmision,
            @NotNull(message = "La capacidad de pasajeros es obligatoria") @Positive(message = "La capacidad de pasajeros debe ser mayor que 0") Integer capacidadPasajeros,
            @NotNull(message = "La tarifa diaria es obligatoria") @Positive(message = "La tarifa diaria debe ser mayor que 0") Double tarifaDiaria,
            @Positive(message = "El kilometraje incluido debe ser mayor que 0 (vacío = ilimitado)") Integer kilometrajeIncluido,
            @NotNull(message = "La franquicia de daños es obligatoria") @PositiveOrZero(message = "La franquicia no puede ser negativa") Double franquiciaDanos,
            @NotBlank(message = "La sucursal es obligatoria") String sucursalId) {
    }

    /** HU-11 */
    public record Reasignacion(@NotBlank(message = "La sucursal es obligatoria") String sucursalId) {
    }

    /** HU-14 */
    public record CambioEstado(@NotNull(message = "El estado es obligatorio") EstadoVehiculo estado) {
    }

    /** HU-04 */
    public record NuevaReserva(
            @NotBlank(message = "La placa es obligatoria") String placa,
            @NotNull(message = "La fecha y hora de recogida es obligatoria") LocalDateTime fechaHoraRecogida,
            @NotNull(message = "La fecha y hora de devolución es obligatoria") LocalDateTime fechaHoraDevolucion,
            @NotBlank(message = "La sucursal de recogida es obligatoria") String sucursalRecogidaId,
            @NotBlank(message = "La sucursal de devolución es obligatoria") String sucursalDevolucionId) {
    }

    /** HU-05 */
    public record AgregarCobertura(@NotBlank(message = "La cobertura es obligatoria") String coberturaId) {
    }

    /** HU-06 */
    public record Confirmacion(@NotBlank(message = "Debes registrar un método de pago") String ultimosDigitos) {
    }

    /** HU-16 */
    public record Entrega(@NotNull(message = "Debe indicar si los documentos son válidos") Boolean documentosValidos) {
    }

    /** HU-17 */
    public record Devolucion(
            @Positive(message = "El valor del daño debe ser mayor que 0") Double valorDano,
            String motivoDano) {
    }
}
