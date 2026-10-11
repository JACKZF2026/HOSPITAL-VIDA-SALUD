package pe.cibertec.vidasalud.notificaciones.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.cibertec.vidasalud.notificaciones.config.RabbitConfig;
import pe.cibertec.vidasalud.notificaciones.model.Notificacion;
import pe.cibertec.vidasalud.notificaciones.repository.NotificacionRepository;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consumidor de eventos de negocio. Genera una notificacion simulada por
 * cada evento recibido y la persiste en su propia base de datos.
 */
@Component
public class NotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionListener.class);

    private final NotificacionRepository repository;

    public NotificacionListener(NotificacionRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void recibir(EventoIntegracion evento) {
        Notificacion notificacion = new Notificacion();
        notificacion.setTipoEvento(evento.tipoEvento());
        notificacion.setOrigen(evento.origen());
        notificacion.setDestinatario(destinatario(evento.datos()));
        notificacion.setMensaje(construirMensaje(evento));
        notificacion.setCanal("INTERNO");
        notificacion.setEstado("GENERADA");
        notificacion.setFechaRecepcion(LocalDateTime.now());

        repository.save(notificacion)
                .doOnSuccess(n -> log.info("Notificacion generada [{}] {}", evento.tipoEvento(), n.getMensaje()))
                .doOnError(e -> log.error("Error guardando notificacion: {}", e.getMessage()))
                .subscribe();
    }

    private String destinatario(Map<String, Object> datos) {
        if (datos == null) {
            return "desconocido";
        }
        Object correo = datos.get("correo");
        if (correo != null) {
            return correo.toString();
        }
        Object pacienteId = datos.get("pacienteId");
        return pacienteId != null ? "paciente:" + pacienteId : "desconocido";
    }

    private String construirMensaje(EventoIntegracion evento) {
        Map<String, Object> d = evento.datos() != null ? evento.datos() : Map.of();
        return switch (evento.tipoEvento()) {
            case "paciente.registrado" -> "Paciente registrado: " + d.get("nombres") + " " + d.get("apellidos");
            case "cita.creada" -> "Cita registrada " + d.get("codigo") + " para " + d.get("fechaHora");
            case "cita.confirmada" -> "Cita confirmada " + d.get("codigo") + " el " + d.get("fechaHora");
            case "cita.cancelada" -> "Cita cancelada " + d.get("codigo");
            case "cita.reprogramada" -> "Cita reprogramada " + d.get("codigo") + " para " + d.get("fechaHora");
            case "cita.atendida" -> "Cita atendida " + d.get("codigo");
            case "cita.ausente" -> "Paciente ausente a la cita " + d.get("codigo");
            case "atencion.registrada" -> "Atencion registrada " + d.get("codigo");
            case "atencion.actualizada" -> "Atencion " + d.get("codigo") + " cambio a " + d.get("estadoNuevo");
            case "laboratorio.actualizado" -> "Solicitud de laboratorio " + d.get("codigo")
                    + " en estado " + d.get("estadoNuevo");
            case "pago.registrado" -> "Pago registrado " + d.get("numeroOperacion") + " por S/ " + d.get("monto");
            case "pago.anulado" -> "Pago anulado " + d.get("numeroOperacion");
            default -> "Evento recibido: " + evento.tipoEvento();
        };
    }
}
