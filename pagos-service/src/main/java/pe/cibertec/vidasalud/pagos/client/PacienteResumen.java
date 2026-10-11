package pe.cibertec.vidasalud.pagos.client;

/**
 * Contrato minimo consumido del servicio de pacientes.
 */
public record PacienteResumen(
        Long id,
        String nombres,
        String apellidos,
        String estado
) {
}
