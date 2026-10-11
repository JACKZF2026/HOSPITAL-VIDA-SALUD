package pe.cibertec.vidasalud.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pe.cibertec.vidasalud.auditoria.listener.AuditoriaListener;
import pe.cibertec.vidasalud.auditoria.listener.EventoIntegracion;
import pe.cibertec.vidasalud.auditoria.model.EventoAuditoria;
import pe.cibertec.vidasalud.auditoria.repository.EventoAuditoriaRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba del componente consumidor de auditoria: cada evento recibido se
 * persiste con origen, recurso, transicion de estado y payload.
 */
class AuditoriaListenerTest {

    @Test
    void eventoRecibidoSePersisteEnAuditoria() {
        EventoAuditoriaRepository repository = mock(EventoAuditoriaRepository.class);
        when(repository.save(any(EventoAuditoria.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        AuditoriaListener listener = new AuditoriaListener(repository, new ObjectMapper());

        String eventoId = UUID.randomUUID().toString();
        EventoIntegracion evento = new EventoIntegracion(
                eventoId,
                "cita.confirmada",
                "citas-service",
                Instant.now(),
                Map.of("citaId", 5L, "estadoAnterior", "PENDIENTE", "estadoNuevo", "CONFIRMADA"));

        listener.recibir(evento);

        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(repository, timeout(2000)).save(captor.capture());

        EventoAuditoria a = captor.getValue();
        assertEquals(eventoId, a.getEventoId());
        assertEquals("cita.confirmada", a.getTipoEvento());
        assertEquals("citas-service", a.getOrigen());
        assertEquals("citaId=5", a.getRecursoId());
        assertEquals("PENDIENTE", a.getEstadoAnterior());
        assertEquals("CONFIRMADA", a.getEstadoNuevo());
        assertTrue(a.getPayload().contains("CONFIRMADA"));
    }
}
