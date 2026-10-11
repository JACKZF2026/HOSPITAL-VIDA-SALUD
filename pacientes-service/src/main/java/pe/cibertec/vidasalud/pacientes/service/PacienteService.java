package pe.cibertec.vidasalud.pacientes.service;

import org.springframework.stereotype.Service;
import pe.cibertec.vidasalud.pacientes.dto.PacienteRequest;
import pe.cibertec.vidasalud.pacientes.events.EventoPublicador;
import pe.cibertec.vidasalud.pacientes.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.pacientes.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.pacientes.model.EstadoPaciente;
import pe.cibertec.vidasalud.pacientes.model.Paciente;
import pe.cibertec.vidasalud.pacientes.repository.PacienteRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class PacienteService {

    private final PacienteRepository repository;
    private final EventoPublicador eventoPublicador;

    public PacienteService(PacienteRepository repository, EventoPublicador eventoPublicador) {
        this.repository = repository;
        this.eventoPublicador = eventoPublicador;
    }

    public Mono<Paciente> registrar(PacienteRequest request) {
        String tipo = normalizar(request.tipoDocumento());
        String numero = request.numeroDocumento().trim();
        return repository.existsByTipoDocumentoAndNumeroDocumento(tipo, numero)
                .flatMap(existe -> {
                    if (Boolean.TRUE.equals(existe)) {
                        return Mono.<Paciente>error(new ReglaNegocioException(
                                "Ya existe un paciente con " + tipo + " " + numero));
                    }
                    return repository.save(mapear(request, tipo, numero));
                })
                .doOnSuccess(this::publicarEventoRegistro);
    }

    public Flux<Paciente> listar(String estado) {
        if (estado == null || estado.isBlank()) {
            return repository.findAll();
        }
        return repository.findByEstado(estado.trim().toUpperCase(Locale.ROOT));
    }

    public Mono<Paciente> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Paciente no encontrado: " + id)));
    }

    public Mono<Paciente> buscarPorDocumento(String tipoDocumento, String numeroDocumento) {
        return repository.findByTipoDocumentoAndNumeroDocumento(normalizar(tipoDocumento), numeroDocumento.trim())
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException(
                        "Paciente no encontrado: " + tipoDocumento + " " + numeroDocumento)));
    }

    public Mono<Paciente> actualizar(Long id, PacienteRequest request) {
        String tipo = normalizar(request.tipoDocumento());
        String numero = request.numeroDocumento().trim();
        return buscarPorId(id)
                .flatMap(actual -> repository
                        .existsByTipoDocumentoAndNumeroDocumentoAndIdNot(tipo, numero, id)
                        .flatMap(existe -> {
                            if (Boolean.TRUE.equals(existe)) {
                                return Mono.<Paciente>error(new ReglaNegocioException(
                                        "Otro paciente ya usa " + tipo + " " + numero));
                            }
                            actual.setTipoDocumento(tipo);
                            actual.setNumeroDocumento(numero);
                            actual.setNombres(request.nombres());
                            actual.setApellidos(request.apellidos());
                            actual.setFechaNacimiento(request.fechaNacimiento());
                            actual.setTelefono(request.telefono());
                            actual.setCorreo(request.correo());
                            actual.setDireccion(request.direccion());
                            return repository.save(actual);
                        }));
    }

    public Mono<Paciente> cambiarEstado(Long id, String nuevoEstado) {
        return Mono.defer(() -> {
                    EstadoPaciente estado;
                    try {
                        estado = EstadoPaciente.valueOf(nuevoEstado.trim().toUpperCase(Locale.ROOT));
                    } catch (RuntimeException e) {
                        return Mono.<EstadoPaciente>error(new ReglaNegocioException(
                                "Estado invalido. Valores permitidos: ACTIVO, INACTIVO"));
                    }
                    return Mono.just(estado);
                })
                .flatMap(estado -> buscarPorId(id)
                        .flatMap(paciente -> {
                            paciente.setEstado(estado.name());
                            return repository.save(paciente);
                        }));
    }

    private Paciente mapear(PacienteRequest r, String tipo, String numero) {
        Paciente p = new Paciente();
        p.setTipoDocumento(tipo);
        p.setNumeroDocumento(numero);
        p.setNombres(r.nombres());
        p.setApellidos(r.apellidos());
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setTelefono(r.telefono());
        p.setCorreo(r.correo());
        p.setDireccion(r.direccion());
        p.setEstado(EstadoPaciente.ACTIVO.name());
        p.setFechaRegistro(LocalDateTime.now());
        return p;
    }

    private void publicarEventoRegistro(Paciente p) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("pacienteId", p.getId());
        datos.put("tipoDocumento", p.getTipoDocumento());
        datos.put("numeroDocumento", p.getNumeroDocumento());
        datos.put("nombres", p.getNombres());
        datos.put("apellidos", p.getApellidos());
        datos.put("correo", p.getCorreo());
        datos.put("estadoNuevo", p.getEstado());
        eventoPublicador.publicar("paciente.registrado", datos);
    }

    private String normalizar(String tipoDocumento) {
        return tipoDocumento.trim().toUpperCase(Locale.ROOT);
    }
}
