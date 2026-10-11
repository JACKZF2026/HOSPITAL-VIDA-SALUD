package pe.cibertec.vidasalud.auditoria.listener;

import java.time.Instant;
import java.util.Map;

/**
 * Copia local del contrato de evento publicado por los microservicios
 * productores. El acoplamiento es al formato del mensaje, no a las clases
 * de otros servicios.
 */
public record EventoIntegracion(
        String eventoId,
        String tipoEvento,
        String origen,
        Instant fechaEvento,
        Map<String, Object> datos
) {
}
