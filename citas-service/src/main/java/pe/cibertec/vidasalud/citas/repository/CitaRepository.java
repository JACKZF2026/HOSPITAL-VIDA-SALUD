package pe.cibertec.vidasalud.citas.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.cibertec.vidasalud.citas.model.Cita;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface CitaRepository extends R2dbcRepository<Cita, Long> {

    Mono<Cita> findByPacienteIdAndFechaHoraAndEstadoNot(Long pacienteId, LocalDateTime fechaHora, String estado);

    Mono<Cita> findByProfesionalCodigoAndFechaHoraAndEstadoNot(String profesionalCodigo, LocalDateTime fechaHora, String estado);

    Mono<Cita> findByPacienteIdAndFechaHoraAndEstadoNotAndIdNot(Long pacienteId, LocalDateTime fechaHora, String estado, Long id);

    Mono<Cita> findByProfesionalCodigoAndFechaHoraAndEstadoNotAndIdNot(String profesionalCodigo, LocalDateTime fechaHora, String estado, Long id);

    Flux<Cita> findByPacienteId(Long pacienteId);

    Flux<Cita> findByEstado(String estado);
}
