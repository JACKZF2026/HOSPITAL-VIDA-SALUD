package pe.cibertec.vidasalud.laboratorio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResultadoRequest(
        @NotBlank(message = "El resultado ficticio es obligatorio")
        @Size(max = 500)
        String resultado
) {
}
