package pe.cibertec.vidasalud.atenciones.client;

/**
 * Contrato minimo consumido del servicio de citas.
 */
public record CitaResumen(
        Long id,
        String codigo,
        Long pacienteId,
        String estado
) {
}
