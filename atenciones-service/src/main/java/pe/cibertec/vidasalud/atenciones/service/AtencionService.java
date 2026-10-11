package pe.cibertec.vidasalud.atenciones.service;

import org.springframework.stereotype.Service;
import pe.cibertec.vidasalud.atenciones.client.CitaClient;
import pe.cibertec.vidasalud.atenciones.dto.AtencionRequest;
import pe.cibertec.vidasalud.atenciones.events.EventoPublicador;
import pe.cibertec.vidasalud.atenciones.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.atenciones.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.atenciones.model.Atencion;
import pe.cibertec.vidasalud.atenciones.model.EstadoAtencion;
import pe.cibertec.vidasalud.atenciones.repository.AtencionRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AtencionService {

    private final AtencionRepository repository;
    private final CitaClient citaClient;
    private final EventoPublicador eventoPublicador;

    public AtencionService(AtencionRepository repository, CitaClient citaClient,
                           EventoPublicador eventoPublicador) {
        this.repository = repository;
        this.citaClient = citaClient;
        this.eventoPublicador = eventoPublicador;
    }

    /**
     * Registra una atencion administrativa.
     * Regla: la cita debe existir (validacion sincrona a citas-service) y estar
     * CONFIRMADA; luego la cita se marca como ATENDIDA en citas-service.
     */
    public Mono<Atencion> registrar(AtencionRequest request) {
        return citaClient.obtenerCita(request.citaId())
                .flatMap(cita -> {
                    if (!"CONFIRMADA".equalsIgnoreCase(cita.estado())) {
                        return Mono.<Atencion>error(new ReglaNegocioException(
                                "La cita " + request.citaId() + " no esta CONFIRMADA (estado actual: "
                                        + cita.estado() + ")"));
                    }
                    return repository.existsByCitaId(request.citaId())
                            .flatMap(existe -> {
                                if (Boolean.TRUE.equals(existe)) {
                                    return Mono.<Atencion>error(new ReglaNegocioException(
                                            "La cita " + request.citaId() + " ya tiene una atencion registrada"));
                                }
                                Atencion atencion = mapear(request, cita.pacienteId());
                                return repository.save(atencion)
                                        .flatMap(guardada -> citaClient.marcarComoAtendida(request.citaId())
                                                .thenReturn(guardada));
                            });
                })
                .doOnSuccess(this::publicarEventoRegistro);
    }

    public Flux<Atencion> listar(Long pacienteId, String estado) {
        if (pacienteId != null) {
            return repository.findByPacienteId(pacienteId);
        }
        if (estado != null && !estado.isBlank()) {
            return repository.findByEstado(estado.trim().toUpperCase(Locale.ROOT));
        }
        return repository.findAll();
    }

    public Mono<Atencion> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Atencion no encontrada: " + id)));
    }

    public Mono<Atencion> cambiarEstado(Long id, String nuevoEstado) {
        return Mono.defer(() -> {
                    EstadoAtencion destino;
                    try {
                        destino = EstadoAtencion.desdeTexto(nuevoEstado);
                    } catch (IllegalArgumentException e) {
                        return Mono.<EstadoAtencion>error(new ReglaNegocioException(e.getMessage()));
                    }
                    return Mono.just(destino);
                })
                .flatMap(destino -> buscarPorId(id)
                        .flatMap(atencion -> {
                            EstadoAtencion actual = EstadoAtencion.valueOf(atencion.getEstado());
                            if (!actual.puedeTransicionarA(destino)) {
                                return Mono.<Atencion>error(new ReglaNegocioException(
                                        "Transicion no permitida: " + actual + " -> " + destino
                                                + ". Permitidas: " + actual.transicionesPermitidas()));
                            }
                            atencion.setEstado(destino.name());
                            atencion.setFechaActualizacion(LocalDateTime.now());
                            String estadoAnterior = actual.name();
                            return repository.save(atencion)
                                    .doOnSuccess(a -> publicarEventoCambio(a, estadoAnterior));
                        }));
    }

    private Atencion mapear(AtencionRequest r, Long pacienteId) {
        Atencion a = new Atencion();
        a.setCodigo("ATE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        a.setCitaId(r.citaId());
        a.setPacienteId(pacienteId);
        a.setArea(r.area());
        a.setEstado(EstadoAtencion.REGISTRADA.name());
        a.setFechaAtencion(LocalDateTime.now());
        a.setFechaActualizacion(LocalDateTime.now());
        return a;
    }

    private void publicarEventoRegistro(Atencion a) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("atencionId", a.getId());
        datos.put("codigo", a.getCodigo());
        datos.put("citaId", a.getCitaId());
        datos.put("pacienteId", a.getPacienteId());
        datos.put("area", a.getArea());
        datos.put("estadoNuevo", a.getEstado());
        eventoPublicador.publicar("atencion.registrada", datos);
    }

    private void publicarEventoCambio(Atencion a, String estadoAnterior) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("atencionId", a.getId());
        datos.put("codigo", a.getCodigo());
        datos.put("citaId", a.getCitaId());
        datos.put("pacienteId", a.getPacienteId());
        datos.put("estadoAnterior", estadoAnterior);
        datos.put("estadoNuevo", a.getEstado());
        eventoPublicador.publicar("atencion.actualizada", datos);
    }
}
