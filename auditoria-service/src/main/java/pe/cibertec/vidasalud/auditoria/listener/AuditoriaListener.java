package pe.cibertec.vidasalud.auditoria.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.cibertec.vidasalud.auditoria.config.RabbitConfig;
import pe.cibertec.vidasalud.auditoria.model.EventoAuditoria;
import pe.cibertec.vidasalud.auditoria.repository.EventoAuditoriaRepository;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consumidor de trazabilidad: persiste cada evento de negocio con su
 * origen, recurso afectado, transicion de estado y carga util completa.
 */
@Component
public class AuditoriaListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaListener.class);

    private final EventoAuditoriaRepository repository;
    private final ObjectMapper objectMapper;

    public AuditoriaListener(EventoAuditoriaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void recibir(EventoIntegracion evento) {
        EventoAuditoria auditoria = new EventoAuditoria();
        auditoria.setEventoId(evento.eventoId());
        auditoria.setTipoEvento(evento.tipoEvento());
        auditoria.setOrigen(evento.origen());
        auditoria.setRecursoId(extraerRecursoId(evento.datos()));
        auditoria.setEstadoAnterior(texto(evento.datos(), "estadoAnterior"));
        auditoria.setEstadoNuevo(texto(evento.datos(), "estadoNuevo"));
        auditoria.setFechaEvento(evento.fechaEvento() != null ? evento.fechaEvento().toString() : null);
        auditoria.setPayload(serializar(evento.datos()));
        auditoria.setFechaRegistro(LocalDateTime.now());

        repository.save(auditoria)
                .doOnSuccess(a -> log.info("Auditoria registrada: {} desde {}", evento.tipoEvento(), evento.origen()))
                .doOnError(e -> log.error("Error guardando auditoria: {}", e.getMessage()))
                .subscribe();
    }

    private String extraerRecursoId(Map<String, Object> datos) {
        if (datos == null) {
            return null;
        }
        return datos.entrySet().stream()
                .filter(e -> e.getKey().endsWith("Id") && e.getValue() != null)
                .map(e -> e.getKey() + "=" + e.getValue())
                .findFirst()
                .orElse(null);
    }

    private String texto(Map<String, Object> datos, String clave) {
        if (datos == null || datos.get(clave) == null) {
            return null;
        }
        return datos.get(clave).toString();
    }

    private String serializar(Map<String, Object> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (Exception e) {
            return String.valueOf(datos);
        }
    }
}
