package pe.cibertec.vidasalud.pagos.dto;

import jakarta.validation.constraints.NotBlank;

public record EstadoRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado
) {
}
