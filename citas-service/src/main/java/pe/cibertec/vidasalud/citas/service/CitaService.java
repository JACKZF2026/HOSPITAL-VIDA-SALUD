package pe.cibertec.vidasalud.citas.service;

import org.springframework.stereotype.Service;
import pe.cibertec.vidasalud.citas.client.PacienteClient;
import pe.cibertec.vidasalud.citas.dto.CitaRequest;
import pe.cibertec.vidasalud.citas.events.EventoPublicador;
import pe.cibertec.vidasalud.citas.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.citas.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.citas.model.Cita;
import pe.cibertec.vidasalud.citas.model.EstadoCita;
import pe.cibertec.vidasalud.citas.repository.CitaRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CitaService {

    private final CitaRepository repository;
    private final PacienteClient pacienteClient;
    private final EventoPublicador eventoPublicador;

    public CitaService(CitaRepository repository, PacienteClient pacienteClient,
                       EventoPublicador eventoPublicador) {
        this.repository = repository;
        this.pacienteClient = pacienteClient;
        this.eventoPublicador = eventoPublicador;
    }

    public Mono<Cita> programar(CitaRequest request) {
        // Comunicacion sincrona: se verifica que el paciente exista y este activo
        return pacienteClient.obtenerPaciente(request.pacienteId())
                .flatMap(paciente -> {
                    if (!"ACTIVO".equalsIgnoreCase(paciente.estado())) {
                        return Mono.<Cita>error(new ReglaNegocioException(
                                "El paciente " + request.pacienteId() + " no esta ACTIVO"));
                    }
                    return verificarDisponibilidad(request.pacienteId(), request.profesionalCodigo(),
                            request.fechaHora(), null)
                            .flatMap(ok -> repository.save(mapear(request)));
                })
                .doOnSuccess(c -> publicarEvento("cita.creada", c, null));
    }

    public Flux<Cita> listar(Long pacienteId, String estado) {
        if (pacienteId != null) {
            return repository.findByPacienteId(pacienteId);
        }
        if (estado != null && !estado.isBlank()) {
            return repository.findByEstado(estado.trim().toUpperCase(java.util.Locale.ROOT));
        }
        return repository.findAll();
    }

    public Mono<Cita> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Cita no encontrada: " + id)));
    }

    public Mono<Cita> cambiarEstado(Long id, String nuevoEstado) {
        return Mono.defer(() -> {
                    EstadoCita destino;
                    try {
                        destino = EstadoCita.desdeTexto(nuevoEstado);
                    } catch (IllegalArgumentException e) {
                        return Mono.<EstadoCita>error(new ReglaNegocioException(e.getMessage()));
                    }
                    return Mono.just(destino);
                })
                .flatMap(destino -> buscarPorId(id)
                        .flatMap(cita -> {
                            EstadoCita actual = EstadoCita.valueOf(cita.getEstado());
                            if (!actual.puedeTransicionarA(destino)) {
                                return Mono.<Cita>error(new ReglaNegocioException(
                                        "Transicion no permitida: " + actual + " -> " + destino
                                                + ". Permitidas: " + actual.transicionesPermitidas()));
                            }
                            cita.setEstado(destino.name());
                            cita.setFechaActualizacion(LocalDateTime.now());
                            String estadoAnterior = actual.name();
                            return repository.save(cita)
                                    .doOnSuccess(c -> publicarEvento(
                                            "cita." + destino.name().toLowerCase(java.util.Locale.ROOT),
                                            c, estadoAnterior));
                        }));
    }

    public Mono<Cita> reprogramar(Long id, LocalDateTime nuevaFechaHora) {
        return buscarPorId(id)
                .flatMap(cita -> {
                    EstadoCita actual = EstadoCita.valueOf(cita.getEstado());
                    if (actual != EstadoCita.PENDIENTE && actual != EstadoCita.CONFIRMADA) {
                        return Mono.<Cita>error(new ReglaNegocioException(
                                "Solo se pueden reprogramar citas PENDIENTE o CONFIRMADA. Estado actual: " + actual));
                    }
                    return verificarDisponibilidad(cita.getPacienteId(), cita.getProfesionalCodigo(),
                            nuevaFechaHora, cita.getId())
                            .flatMap(ok -> {
                                cita.setFechaHora(nuevaFechaHora);
                                // al reprogramar vuelve a PENDIENTE para nueva confirmacion
                                cita.setEstado(EstadoCita.PENDIENTE.name());
                                cita.setFechaActualizacion(LocalDateTime.now());
                                return repository.save(cita);
                            });
                })
                .doOnSuccess(c -> publicarEvento("cita.reprogramada", c, null));
    }

    private Mono<Boolean> verificarDisponibilidad(Long pacienteId, String profesionalCodigo,
                                                  LocalDateTime fechaHora, Long excluirId) {
        Mono<Cita> conflictoPaciente = (excluirId == null)
                ? repository.findByPacienteIdAndFechaHoraAndEstadoNot(pacienteId, fechaHora, EstadoCita.CANCELADA.name())
                : repository.findByPacienteIdAndFechaHoraAndEstadoNotAndIdNot(pacienteId, fechaHora, EstadoCita.CANCELADA.name(), excluirId);
        Mono<Cita> conflictoProfesional = (excluirId == null)
                ? repository.findByProfesionalCodigoAndFechaHoraAndEstadoNot(profesionalCodigo, fechaHora, EstadoCita.CANCELADA.name())
                : repository.findByProfesionalCodigoAndFechaHoraAndEstadoNotAndIdNot(profesionalCodigo, fechaHora, EstadoCita.CANCELADA.name(), excluirId);

        return conflictoPaciente
                .flatMap(c -> Mono.<Boolean>error(new ReglaNegocioException(
                        "El paciente ya tiene una cita activa en esa fecha y hora")))
                .switchIfEmpty(Mono.just(true))
                .flatMap(ok -> conflictoProfesional
                        .flatMap(c -> Mono.<Boolean>error(new ReglaNegocioException(
                                "La agenda del profesional ya tiene una cita en esa fecha y hora")))
                        .switchIfEmpty(Mono.just(true)));
    }

    private Cita mapear(CitaRequest r) {
        Cita c = new Cita();
        c.setCodigo("CIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT));
        c.setPacienteId(r.pacienteId());
        c.setEspecialidad(r.especialidad());
        c.setFechaHora(r.fechaHora());
        c.setProfesionalCodigo(r.profesionalCodigo());
        c.setMotivo(r.motivo());
        c.setEstado(EstadoCita.PENDIENTE.name());
        c.setFechaCreacion(LocalDateTime.now());
        c.setFechaActualizacion(LocalDateTime.now());
        return c;
    }

    private void publicarEvento(String tipoEvento, Cita c, String estadoAnterior) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("citaId", c.getId());
        datos.put("codigo", c.getCodigo());
        datos.put("pacienteId", c.getPacienteId());
        datos.put("especialidad", c.getEspecialidad());
        datos.put("fechaHora", c.getFechaHora() != null ? c.getFechaHora().toString() : null);
        datos.put("profesionalCodigo", c.getProfesionalCodigo());
        datos.put("estadoNuevo", c.getEstado());
        if (estadoAnterior != null) {
            datos.put("estadoAnterior", estadoAnterior);
        }
        eventoPublicador.publicar(tipoEvento, datos);
    }
}
