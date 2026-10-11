package pe.cibertec.vidasalud.citas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.cibertec.vidasalud.citas.client.PacienteClient;
import pe.cibertec.vidasalud.citas.client.PacienteResumen;
import pe.cibertec.vidasalud.citas.dto.CitaRequest;
import pe.cibertec.vidasalud.citas.events.EventoPublicador;
import pe.cibertec.vidasalud.citas.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.citas.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.citas.model.Cita;
import pe.cibertec.vidasalud.citas.model.EstadoCita;
import pe.cibertec.vidasalud.citas.repository.CitaRepository;
import pe.cibertec.vidasalud.citas.service.CitaService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    CitaRepository repository;

    @Mock
    PacienteClient pacienteClient;

    @Mock
    EventoPublicador eventoPublicador;

    @InjectMocks
    CitaService service;

    private CitaRequest requestValido() {
        return new CitaRequest(1L, "Medicina General",
                LocalDateTime.now().plusDays(2), "PRO-01", "Consulta de control");
    }

    private Cita citaEnEstado(EstadoCita estado) {
        Cita c = new Cita();
        c.setId(1L);
        c.setCodigo("CIT-12345678");
        c.setPacienteId(1L);
        c.setEspecialidad("Medicina General");
        c.setFechaHora(LocalDateTime.now().plusDays(2));
        c.setProfesionalCodigo("PRO-01");
        c.setEstado(estado.name());
        return c;
    }

    @Test
    void programarCitaConPacienteValidoGuardaYPublica() {
        when(pacienteClient.obtenerPaciente(1L))
                .thenReturn(Mono.just(new PacienteResumen(1L, "Juan", "Perez", "ACTIVO")));
        when(repository.findByPacienteIdAndFechaHoraAndEstadoNot(any(), any(), any()))
                .thenReturn(Mono.empty());
        when(repository.findByProfesionalCodigoAndFechaHoraAndEstadoNot(any(), any(), any()))
                .thenReturn(Mono.empty());
        when(repository.save(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setId(10L);
            return Mono.just(c);
        });

        StepVerifier.create(service.programar(requestValido()))
                .expectNextMatches(c -> c.getId() == 10L
                        && EstadoCita.PENDIENTE.name().equals(c.getEstado())
                        && c.getCodigo().startsWith("CIT-"))
                .verifyComplete();

        verify(eventoPublicador).publicar(eq("cita.creada"), anyMap());
    }

    @Test
    void programarCitaConPacienteInexistenteEsRechazada() {
        when(pacienteClient.obtenerPaciente(1L))
                .thenReturn(Mono.error(new RecursoNoEncontradoException("Paciente no encontrado: 1")));

        StepVerifier.create(service.programar(requestValido()))
                .expectError(RecursoNoEncontradoException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void programarCitaConPacienteInactivoEsRechazada() {
        when(pacienteClient.obtenerPaciente(1L))
                .thenReturn(Mono.just(new PacienteResumen(1L, "Juan", "Perez", "INACTIVO")));

        StepVerifier.create(service.programar(requestValido()))
                .expectError(ReglaNegocioException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void programarCitaEnHorarioOcupadoEsRechazada() {
        when(pacienteClient.obtenerPaciente(1L))
                .thenReturn(Mono.just(new PacienteResumen(1L, "Juan", "Perez", "ACTIVO")));
        when(repository.findByPacienteIdAndFechaHoraAndEstadoNot(any(), any(), any()))
                .thenReturn(Mono.just(citaEnEstado(EstadoCita.CONFIRMADA)));

        StepVerifier.create(service.programar(requestValido()))
                .expectErrorMatches(e -> e instanceof ReglaNegocioException
                        && e.getMessage().contains("ya tiene una cita"))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void confirmarCitaPendientePublicaEventoCitaConfirmada() {
        Cita cita = citaEnEstado(EstadoCita.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Mono.just(cita));
        when(repository.save(any(Cita.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.cambiarEstado(1L, "CONFIRMADA"))
                .expectNextMatches(c -> EstadoCita.CONFIRMADA.name().equals(c.getEstado()))
                .verifyComplete();

        verify(eventoPublicador).publicar(eq("cita.confirmada"), anyMap());
    }

    @Test
    void confirmarCitaCanceladaEsRechazado() {
        when(repository.findById(1L)).thenReturn(Mono.just(citaEnEstado(EstadoCita.CANCELADA)));

        StepVerifier.create(service.cambiarEstado(1L, "CONFIRMADA"))
                .expectErrorMatches(e -> e instanceof ReglaNegocioException
                        && e.getMessage().contains("Transicion no permitida"))
                .verify();

        verify(repository, never()).save(any());
        verify(eventoPublicador, never()).publicar(any(), anyMap());
    }

    @Test
    void cambiarEstadoDeCitaInexistenteDevuelve404() {
        when(repository.findById(77L)).thenReturn(Mono.empty());

        StepVerifier.create(service.cambiarEstado(77L, "CONFIRMADA"))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
    }
}
