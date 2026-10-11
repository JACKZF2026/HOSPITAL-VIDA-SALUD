package pe.cibertec.vidasalud.laboratorio.service;

import org.springframework.stereotype.Service;
import pe.cibertec.vidasalud.laboratorio.client.AtencionClient;
import pe.cibertec.vidasalud.laboratorio.dto.SolicitudRequest;
import pe.cibertec.vidasalud.laboratorio.events.EventoPublicador;
import pe.cibertec.vidasalud.laboratorio.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.laboratorio.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.laboratorio.model.EstadoSolicitud;
import pe.cibertec.vidasalud.laboratorio.model.SolicitudLaboratorio;
import pe.cibertec.vidasalud.laboratorio.repository.SolicitudLaboratorioRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class SolicitudLaboratorioService {

    private final SolicitudLaboratorioRepository repository;
    private final AtencionClient atencionClient;
    private final EventoPublicador eventoPublicador;

    public SolicitudLaboratorioService(SolicitudLaboratorioRepository repository,
                                       AtencionClient atencionClient,
                                       EventoPublicador eventoPublicador) {
        this.repository = repository;
        this.atencionClient = atencionClient;
        this.eventoPublicador = eventoPublicador;
    }

    /**
     * Registra una solicitud de laboratorio ficticia.
     * Regla: la atencion debe existir (validacion sincrona a atenciones-service).
     * Los resultados registrados son siempre ficticios, sin valor clinico.
     */
    public Mono<SolicitudLaboratorio> registrar(SolicitudRequest request) {
        return atencionClient.obtenerAtencion(request.atencionId())
                .flatMap(atencion -> repository.save(mapear(request, atencion.pacienteId())))
                .doOnSuccess(s -> publicarEvento("laboratorio.actualizado", s, null));
    }

    public Flux<SolicitudLaboratorio> listar(Long pacienteId, Long atencionId, String estado) {
        if (atencionId != null) {
            return repository.findByAtencionId(atencionId);
        }
        if (pacienteId != null) {
            return repository.findByPacienteId(pacienteId);
        }
        if (estado != null && !estado.isBlank()) {
            return repository.findByEstado(estado.trim().toUpperCase(Locale.ROOT));
        }
        return repository.findAll();
    }

    public Mono<SolicitudLaboratorio> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Solicitud no encontrada: " + id)));
    }

    public Mono<SolicitudLaboratorio> cambiarEstado(Long id, String nuevoEstado) {
        return Mono.defer(() -> {
                    EstadoSolicitud destino;
                    try {
                        destino = EstadoSolicitud.desdeTexto(nuevoEstado);
                    } catch (IllegalArgumentException e) {
                        return Mono.<EstadoSolicitud>error(new ReglaNegocioException(e.getMessage()));
                    }
                    return Mono.just(destino);
                })
                .flatMap(destino -> buscarPorId(id)
                        .flatMap(solicitud -> {
                            EstadoSolicitud actual = EstadoSolicitud.valueOf(solicitud.getEstado());
                            if (!actual.puedeTransicionarA(destino)) {
                                return Mono.<SolicitudLaboratorio>error(new ReglaNegocioException(
                                        "Transicion no permitida: " + actual + " -> " + destino
                                                + ". Permitidas: " + actual.transicionesPermitidas()));
                            }
                            solicitud.setEstado(destino.name());
                            solicitud.setFechaActualizacion(LocalDateTime.now());
                            String estadoAnterior = actual.name();
                            return repository.save(solicitud)
                                    .doOnSuccess(s -> publicarEvento("laboratorio.actualizado", s, estadoAnterior));
                        }));
    }

    /**
     * Registra un resultado ficticio. Solo permitido cuando la solicitud
     * esta EN_PROCESO; al guardar el resultado pasa a COMPLETADO.
     */
    public Mono<SolicitudLaboratorio> registrarResultado(Long id, String resultado) {
        return buscarPorId(id)
                .flatMap(solicitud -> {
                    EstadoSolicitud actual = EstadoSolicitud.valueOf(solicitud.getEstado());
                    if (actual != EstadoSolicitud.EN_PROCESO) {
                        return Mono.<SolicitudLaboratorio>error(new ReglaNegocioException(
                                "Solo se puede registrar resultado en estado EN_PROCESO. Estado actual: " + actual));
                    }
                    solicitud.setResultadoFicticio(resultado);
                    solicitud.setEstado(EstadoSolicitud.COMPLETADO.name());
                    solicitud.setFechaActualizacion(LocalDateTime.now());
                    return repository.save(solicitud)
                            .doOnSuccess(s -> publicarEvento("laboratorio.actualizado", s, EstadoSolicitud.EN_PROCESO.name()));
                });
    }

    private SolicitudLaboratorio mapear(SolicitudRequest r, Long pacienteId) {
        SolicitudLaboratorio s = new SolicitudLaboratorio();
        s.setCodigo("LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        s.setAtencionId(r.atencionId());
        s.setPacienteId(pacienteId);
        s.setTipoExamen(r.tipoExamen());
        s.setEstado(EstadoSolicitud.SOLICITADO.name());
        s.setFechaSolicitud(LocalDateTime.now());
        s.setFechaActualizacion(LocalDateTime.now());
        return s;
    }

    private void publicarEvento(String tipoEvento, SolicitudLaboratorio s, String estadoAnterior) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("solicitudId", s.getId());
        datos.put("codigo", s.getCodigo());
        datos.put("pacienteId", s.getPacienteId());
        datos.put("atencionId", s.getAtencionId());
        datos.put("tipoExamen", s.getTipoExamen());
        if (estadoAnterior != null) {
            datos.put("estadoAnterior", estadoAnterior);
        }
        datos.put("estadoNuevo", s.getEstado());
        eventoPublicador.publicar(tipoEvento, datos);
    }
}
