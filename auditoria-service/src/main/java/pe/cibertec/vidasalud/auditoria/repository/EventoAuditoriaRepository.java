package pe.cibertec.vidasalud.auditoria.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.cibertec.vidasalud.auditoria.model.EventoAuditoria;
import reactor.core.publisher.Flux;

public interface EventoAuditoriaRepository extends R2dbcRepository<EventoAuditoria, Long> {

    Flux<EventoAuditoria> findByTipoEvento(String tipoEvento);

    Flux<EventoAuditoria> findByRecursoId(String recursoId);
}
