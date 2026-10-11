package pe.cibertec.vidasalud.notificaciones.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.cibertec.vidasalud.notificaciones.model.Notificacion;
import reactor.core.publisher.Flux;

public interface NotificacionRepository extends R2dbcRepository<Notificacion, Long> {

    Flux<Notificacion> findByTipoEvento(String tipoEvento);
}
