package pe.cibertec.vidasalud.laboratorio.client;

/**
 * Contrato minimo consumido del servicio de atenciones.
 */
public record AtencionResumen(
        Long id,
        String codigo,
        Long citaId,
        Long pacienteId,
        String estado
) {
}
