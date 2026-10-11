package pe.cibertec.vidasalud.pacientes.events;

import java.time.Instant;
import java.util.Map;

/**
 * Contrato de evento de integracion publicado en RabbitMQ.
 * Los consumidores (notificaciones, auditoria) poseen su propia copia del
 * contrato: los servicios se acoplan al formato del mensaje, no entre ellos.
 */
public record EventoIntegracion(
        String eventoId,
        String tipoEvento,
        String origen,
        Instant fechaEvento,
        Map<String, Object> datos
) {
}
