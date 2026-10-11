package pe.cibertec.vidasalud.citas.events;

import java.time.Instant;
import java.util.Map;

/**
 * Contrato de evento de integracion publicado en RabbitMQ.
 */
public record EventoIntegracion(
        String eventoId,
        String tipoEvento,
        String origen,
        Instant fechaEvento,
        Map<String, Object> datos
) {
}
