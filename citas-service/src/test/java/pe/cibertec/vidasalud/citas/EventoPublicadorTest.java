package pe.cibertec.vidasalud.citas;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import pe.cibertec.vidasalud.citas.config.RabbitConfig;
import pe.cibertec.vidasalud.citas.events.EventoIntegracion;
import pe.cibertec.vidasalud.citas.events.EventoPublicador;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Prueba del componente de mensajeria: verifica que el evento se envia al
 * exchange hospital.eventos con la routing key igual al tipo de evento y
 * con el contrato EventoIntegracion serializado.
 */
class EventoPublicadorTest {

    @Test
    void publicarEnviaEventoAlExchangeConRoutingKey() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        EventoPublicador publicador = new EventoPublicador(rabbitTemplate);

        publicador.publicar("cita.confirmada", Map.of("citaId", 5L, "codigo", "CIT-ABCDEF12"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitConfig.EXCHANGE),
                org.mockito.ArgumentMatchers.eq("cita.confirmada"),
                captor.capture());

        EventoIntegracion evento = (EventoIntegracion) captor.getValue();
        assertEquals("cita.confirmada", evento.tipoEvento());
        assertEquals("citas-service", evento.origen());
        assertEquals(5L, evento.datos().get("citaId"));
        assertNotNull(evento.eventoId());
        assertNotNull(evento.fechaEvento());
    }
}
