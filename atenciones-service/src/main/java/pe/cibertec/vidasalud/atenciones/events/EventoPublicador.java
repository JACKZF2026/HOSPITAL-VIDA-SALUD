package pe.cibertec.vidasalud.atenciones.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import pe.cibertec.vidasalud.atenciones.config.RabbitConfig;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
                "atenciones-service",
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
