package pe.cibertec.vidasalud.pagos.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.cibertec.vidasalud.pagos.model.Pago;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PagoRepository extends R2dbcRepository<Pago, Long> {

    Mono<Boolean> existsByNumeroOperacion(String numeroOperacion);

    Flux<Pago> findByPacienteId(Long pacienteId);

    Flux<Pago> findByEstado(String estado);
}
