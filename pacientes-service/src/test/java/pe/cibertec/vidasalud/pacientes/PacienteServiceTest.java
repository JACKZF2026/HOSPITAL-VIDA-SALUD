package pe.cibertec.vidasalud.pacientes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.cibertec.vidasalud.pacientes.dto.PacienteRequest;
import pe.cibertec.vidasalud.pacientes.events.EventoPublicador;
import pe.cibertec.vidasalud.pacientes.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.pacientes.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.pacientes.model.Paciente;
import pe.cibertec.vidasalud.pacientes.repository.PacienteRepository;
import pe.cibertec.vidasalud.pacientes.service.PacienteService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    PacienteRepository repository;

    @Mock
    EventoPublicador eventoPublicador;

    @InjectMocks
    PacienteService service;

    private PacienteRequest requestValido() {
        return new PacienteRequest("DNI", "12345678", "Juan", "Perez Prueba",
                LocalDate.of(1990, 5, 10), "999000111", "juan.prueba@example.com", "Av. Ficticia 123");
    }

    @Test
    void registrarPacienteValidoGuardaYPublicaEvento() {
        Paciente guardado = new Paciente();
        guardado.setId(1L);
        guardado.setEstado("ACTIVO");

        when(repository.existsByTipoDocumentoAndNumeroDocumento("DNI", "12345678"))
                .thenReturn(Mono.just(false));
        when(repository.save(any(Paciente.class))).thenReturn(Mono.just(guardado));

        StepVerifier.create(service.registrar(requestValido()))
                .expectNextMatches(p -> p.getId() == 1L && "ACTIVO".equals(p.getEstado()))
                .verifyComplete();

        verify(eventoPublicador).publicar(eq("paciente.registrado"), anyMap());
    }

    @Test
    void registrarPacienteDuplicadoEsRechazado() {
        when(repository.existsByTipoDocumentoAndNumeroDocumento("DNI", "12345678"))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(requestValido()))
                .expectErrorMatches(e -> e instanceof ReglaNegocioException
                        && e.getMessage().contains("Ya existe"))
                .verify();

        verify(repository, never()).save(any());
        verify(eventoPublicador, never()).publicar(any(), anyMap());
    }

    @Test
    void buscarPorIdInexistenteLanza404() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.buscarPorId(99L))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
    }

    @Test
    void cambiarEstadoInvalidoEsRechazado() {
        StepVerifier.create(service.cambiarEstado(1L, "BORRADO"))
                .expectError(ReglaNegocioException.class)
                .verify();
    }
}
