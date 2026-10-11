package pe.cibertec.vidasalud.pagos.service;

import org.springframework.stereotype.Service;
import pe.cibertec.vidasalud.pagos.client.PacienteClient;
import pe.cibertec.vidasalud.pagos.dto.PagoRequest;
import pe.cibertec.vidasalud.pagos.events.EventoPublicador;
import pe.cibertec.vidasalud.pagos.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.pagos.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.pagos.model.EstadoPago;
import pe.cibertec.vidasalud.pagos.model.Pago;
import pe.cibertec.vidasalud.pagos.repository.PagoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class PagoService {

    private final PagoRepository repository;
    private final PacienteClient pacienteClient;
    private final EventoPublicador eventoPublicador;

    public PagoService(PagoRepository repository, PacienteClient pacienteClient,
                       EventoPublicador eventoPublicador) {
        this.repository = repository;
        this.pacienteClient = pacienteClient;
        this.eventoPublicador = eventoPublicador;
    }

    /**
     * Registra un pago simulado.
     * Reglas: monto > 0, numero de operacion unico, paciente existente
     * (validacion sincrona a pacientes-service).
     */
    public Mono<Pago> registrar(PagoRequest request) {
        if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new ReglaNegocioException(
                    "El monto del pago debe ser mayor que cero: " + request.monto()));
        }
        return repository.existsByNumeroOperacion(request.numeroOperacion().trim())
                .flatMap(existe -> {
                    if (Boolean.TRUE.equals(existe)) {
                        return Mono.<Pago>error(new ReglaNegocioException(
                                "Ya existe un pago con el numero de operacion "
                                        + request.numeroOperacion()));
                    }
                    return pacienteClient.obtenerPaciente(request.pacienteId())
                            .flatMap(paciente -> repository.save(mapear(request)));
                })
                .doOnSuccess(p -> publicarEvento("pago.registrado", p, null));
    }

    public Flux<Pago> listar(Long pacienteId, String estado) {
        if (pacienteId != null) {
            return repository.findByPacienteId(pacienteId);
        }
        if (estado != null && !estado.isBlank()) {
            return repository.findByEstado(estado.trim().toUpperCase(Locale.ROOT));
        }
        return repository.findAll();
    }

    public Mono<Pago> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Pago no encontrado: " + id)));
    }

    public Mono<Pago> cambiarEstado(Long id, String nuevoEstado) {
        return Mono.defer(() -> {
                    EstadoPago destino;
                    try {
                        destino = EstadoPago.desdeTexto(nuevoEstado);
                    } catch (IllegalArgumentException e) {
                        return Mono.<EstadoPago>error(new ReglaNegocioException(e.getMessage()));
                    }
                    return Mono.just(destino);
                })
                .flatMap(destino -> buscarPorId(id)
                        .flatMap(pago -> {
                            EstadoPago actual = EstadoPago.valueOf(pago.getEstado());
                            if (!actual.puedeTransicionarA(destino)) {
                                return Mono.<Pago>error(new ReglaNegocioException(
                                        "Transicion no permitida: " + actual + " -> " + destino));
                            }
                            pago.setEstado(destino.name());
                            pago.setFechaActualizacion(LocalDateTime.now());
                            String estadoAnterior = actual.name();
                            return repository.save(pago)
                                    .doOnSuccess(p -> publicarEvento("pago.anulado", p, estadoAnterior));
                        }));
    }

    private Pago mapear(PagoRequest r) {
        Pago p = new Pago();
        p.setNumeroOperacion(r.numeroOperacion().trim());
        p.setPacienteId(r.pacienteId());
        p.setConcepto(r.concepto());
        p.setMonto(r.monto());
        p.setMedioPago(r.medioPago().trim().toUpperCase(Locale.ROOT));
        p.setEstado(EstadoPago.REGISTRADO.name());
        p.setFechaPago(LocalDateTime.now());
        p.setFechaActualizacion(LocalDateTime.now());
        return p;
    }

    private void publicarEvento(String tipoEvento, Pago p, String estadoAnterior) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("pagoId", p.getId());
        datos.put("numeroOperacion", p.getNumeroOperacion());
        datos.put("pacienteId", p.getPacienteId());
        datos.put("concepto", p.getConcepto());
        datos.put("monto", p.getMonto() != null ? p.getMonto().toPlainString() : null);
        datos.put("medioPago", p.getMedioPago());
        if (estadoAnterior != null) {
            datos.put("estadoAnterior", estadoAnterior);
        }
        datos.put("estadoNuevo", p.getEstado());
        eventoPublicador.publicar(tipoEvento, datos);
    }
}
