package pe.cibertec.vidasalud.notificaciones;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pe.cibertec.vidasalud.notificaciones.listener.EventoIntegracion;
import pe.cibertec.vidasalud.notificaciones.listener.NotificacionListener;
import pe.cibertec.vidasalud.notificaciones.model.Notificacion;
import pe.cibertec.vidasalud.notificaciones.repository.NotificacionRepository;
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
 * Prueba del componente consumidor: al recibir un evento CitaConfirmada se
 * genera y persiste una notificacion con el mensaje correspondiente.
 */
class NotificacionListenerTest {

    @Test
    void eventoCitaConfirmadaGeneraNotificacion() {
        NotificacionRepository repository = mock(NotificacionRepository.class);
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        NotificacionListener listener = new NotificacionListener(repository);

        EventoIntegracion evento = new EventoIntegracion(
                UUID.randomUUID().toString(),
                "cita.confirmada",
                "citas-service",
                Instant.now(),
                Map.of("citaId", 5L, "codigo", "CIT-ABCDEF12", "pacienteId", 3L));

        listener.recibir(evento);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository, timeout(2000)).save(captor.capture());

        Notificacion n = captor.getValue();
        assertEquals("cita.confirmada", n.getTipoEvento());
        assertEquals("citas-service", n.getOrigen());
        assertEquals("paciente:3", n.getDestinatario());
        assertTrue(n.getMensaje().contains("Cita confirmada"));
        assertTrue(n.getMensaje().contains("CIT-ABCDEF12"));
        assertEquals("GENERADA", n.getEstado());
    }
}
