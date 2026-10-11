package pe.cibertec.vidasalud.pacientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Pattern(regexp = "DNI|CE|PASAPORTE", message = "Tipo de documento debe ser DNI, CE o PASAPORTE")
        String tipoDocumento,

        @NotBlank(message = "El numero de documento es obligatorio")
        @Size(min = 4, max = 20, message = "El numero de documento debe tener entre 4 y 20 caracteres")
        @Pattern(regexp = "^[0-9A-Za-z-]+$", message = "El numero de documento solo admite letras, digitos y guiones")
        String numeroDocumento,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100)
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100)
        String apellidos,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe estar en el pasado")
        LocalDate fechaNacimiento,

        @Size(max = 20)
        String telefono,

        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 120)
        String correo,

        @Size(max = 200)
        String direccion
) {
}
