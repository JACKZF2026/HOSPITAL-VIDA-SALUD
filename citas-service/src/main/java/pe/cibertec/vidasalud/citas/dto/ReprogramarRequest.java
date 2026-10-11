package pe.cibertec.vidasalud.citas.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramarRequest(
        @NotNull(message = "La nueva fecha y hora son obligatorias")
        @Future(message = "La nueva fecha debe ser futura")
        LocalDateTime fechaHora
) {
}
