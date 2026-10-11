package pe.cibertec.vidasalud.laboratorio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRequest(
        @NotNull(message = "La atencion relacionada es obligatoria")
        Long atencionId,

        @NotBlank(message = "El tipo de examen es obligatorio")
        @Size(max = 80)
        String tipoExamen
) {
}
