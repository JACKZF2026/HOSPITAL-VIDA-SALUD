package pe.cibertec.vidasalud.atenciones.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.cibertec.vidasalud.atenciones.model.Atencion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AtencionRepository extends R2dbcRepository<Atencion, Long> {

    Mono<Boolean> existsByCitaId(Long citaId);

    Flux<Atencion> findByPacienteId(Long pacienteId);

    Flux<Atencion> findByEstado(String estado);
}
