package pe.cibertec.vidasalud.pacientes.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import pe.cibertec.vidasalud.pacientes.config.RabbitConfig;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publica eventos de negocio en el exchange topic hospital.eventos.
 * La publicacion es de mejor esfuerzo: si el broker no esta disponible se
 * registra la advertencia y la operacion principal no se ve afectada.
 */
@Component
public class EventoPublicador {

    private static final Logger log = LoggerFactory.getLogger(EventoPublicador.class);

    private final RabbitTemplate rabbitTemplate;

    public EventoPublicador(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(String tipoEvento, Map<String, Object> datos) {
        EventoIntegracion evento = new EventoIntegracion(
                UUID.randomUUID().toString(),
                tipoEvento,
                "pacientes-service",
                Instant.now(),
                datos);
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, tipoEvento, evento);
            log.info("Evento publicado: {} id={}", tipoEvento, evento.eventoId());
        } catch (AmqpException e) {
            log.warn("RabbitMQ no disponible, evento {} no publicado: {}", tipoEvento, e.getMessage());
        }
    }
}
