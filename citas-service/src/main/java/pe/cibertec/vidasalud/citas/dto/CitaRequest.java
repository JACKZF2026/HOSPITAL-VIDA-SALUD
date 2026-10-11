package pe.cibertec.vidasalud.citas.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CitaRequest(
        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @NotBlank(message = "La especialidad es obligatoria")
        @Size(max = 80)
        String especialidad,

        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La fecha de la cita debe ser futura")
        LocalDateTime fechaHora,

        @NotBlank(message = "El codigo del profesional o agenda es obligatorio")
        @Size(max = 40)
        String profesionalCodigo,

        @Size(max = 200)
        String motivo
) {
}
