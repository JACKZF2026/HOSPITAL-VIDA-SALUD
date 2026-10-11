package pe.cibertec.vidasalud.pacientes.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.cibertec.vidasalud.pacientes.model.Paciente;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PacienteRepository extends R2dbcRepository<Paciente, Long> {

    Mono<Boolean> existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    Mono<Boolean> existsByTipoDocumentoAndNumeroDocumentoAndIdNot(String tipoDocumento, String numeroDocumento, Long id);

    Mono<Paciente> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    Flux<Paciente> findByEstado(String estado);
}
