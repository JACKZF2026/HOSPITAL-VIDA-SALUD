package pe.cibertec.vidasalud.laboratorio.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.cibertec.vidasalud.laboratorio.model.SolicitudLaboratorio;
import reactor.core.publisher.Flux;

public interface SolicitudLaboratorioRepository extends R2dbcRepository<SolicitudLaboratorio, Long> {

    Flux<SolicitudLaboratorio> findByPacienteId(Long pacienteId);

    Flux<SolicitudLaboratorio> findByAtencionId(Long atencionId);

    Flux<SolicitudLaboratorio> findByEstado(String estado);
}
