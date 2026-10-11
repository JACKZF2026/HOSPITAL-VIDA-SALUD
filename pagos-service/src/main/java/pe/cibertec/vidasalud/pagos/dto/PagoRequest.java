package pe.cibertec.vidasalud.pagos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PagoRequest(
        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @NotBlank(message = "El concepto es obligatorio")
        @Size(max = 150)
        String concepto,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        BigDecimal monto,

        @NotBlank(message = "El medio de pago es obligatorio")
        @Pattern(regexp = "EFECTIVO|TARJETA|YAPE|TRANSFERENCIA",
                message = "Medio de pago debe ser EFECTIVO, TARJETA, YAPE o TRANSFERENCIA")
        String medioPago,

        @NotBlank(message = "El numero de operacion es obligatorio")
        @Size(min = 4, max = 40)
        String numeroOperacion
) {
}
