package pe.cibertec.vidasalud.atenciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtencionRequest(
        @NotNull(message = "La cita relacionada es obligatoria")
        Long citaId,

        @NotBlank(message = "El area es obligatoria")
        @Size(max = 60)
        String area
) {
}
