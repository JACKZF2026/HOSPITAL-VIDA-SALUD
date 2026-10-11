package pe.cibertec.vidasalud.citas.client;

/**
 * Contrato minimo consumido del servicio de pacientes.
 * Solo se declaran los campos necesarios; el resto del cuerpo se ignora.
 */
public record PacienteResumen(
        Long id,
        String nombres,
        String apellidos,
        String estado
) {
}
