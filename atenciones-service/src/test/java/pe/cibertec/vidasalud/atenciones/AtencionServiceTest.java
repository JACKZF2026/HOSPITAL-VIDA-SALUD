package pe.cibertec.vidasalud.atenciones;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.cibertec.vidasalud.atenciones.client.CitaClient;
import pe.cibertec.vidasalud.atenciones.client.CitaResumen;
import pe.cibertec.vidasalud.atenciones.dto.AtencionRequest;
import pe.cibertec.vidasalud.atenciones.events.EventoPublicador;
import pe.cibertec.vidasalud.atenciones.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.atenciones.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.atenciones.model.Atencion;
import pe.cibertec.vidasalud.atenciones.model.EstadoAtencion;
import pe.cibertec.vidasalud.atenciones.repository.AtencionRepository;
import pe.cibertec.vidasalud.atenciones.service.AtencionService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtencionServiceTest {

    @Mock
    AtencionRepository repository;

    @Mock
    CitaClient citaClient;

    @Mock
    EventoPublicador eventoPublicador;

    @InjectMocks
    AtencionService service;

    @Test
    void registrarAtencionConCitaConfirmadaGuardaYMarcaCita() {
        when(citaClient.obtenerCita(1L))
                .thenReturn(Mono.just(new CitaResumen(1L, "CIT-AAA", 7L, "CONFIRMADA")));
        when(repository.existsByCitaId(1L)).thenReturn(Mono.just(false));
        when(repository.save(any(Atencion.class))).thenAnswer(inv -> {
            Atencion a = inv.getArgument(0);
            a.setId(3L);
            return Mono.just(a);
        });
        when(citaClient.marcarComoAtendida(1L)).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(new AtencionRequest(1L, "Consultorio")))
                .expectNextMatches(a -> a.getId() == 3L
                        && a.getPacienteId() == 7L
                        && EstadoAtencion.REGISTRADA.name().equals(a.getEstado()))
                .verifyComplete();

        // comunicacion sincrona: la cita quedo marcada como ATENDIDA
        verify(citaClient).marcarComoAtendida(1L);
        verify(eventoPublicador).publicar(eq("atencion.registrada"), anyMap());
    }

    @Test
    void registrarAtencionConCitaPendienteEsRechazada() {
        when(citaClient.obtenerCita(1L))
                .thenReturn(Mono.just(new CitaResumen(1L, "CIT-AAA", 7L, "PENDIENTE")));

        StepVerifier.create(service.registrar(new AtencionRequest(1L, "Consultorio")))
                .expectErrorMatches(e -> e instanceof ReglaNegocioException
                        && e.getMessage().contains("no esta CONFIRMADA"))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void registrarAtencionConCitaInexistenteEsRechazada() {
        when(citaClient.obtenerCita(99L))
                .thenReturn(Mono.error(new RecursoNoEncontradoException("Cita no encontrada: 99")));

        StepVerifier.create(service.registrar(new AtencionRequest(99L, "Consultorio")))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
    }

    @Test
    void transicionDeEstadoNoPermitidaEsRechazada() {
        Atencion a = new Atencion();
        a.setId(1L);
        a.setEstado(EstadoAtencion.CANCELADA.name());
        when(repository.findById(1L)).thenReturn(Mono.just(a));

        StepVerifier.create(service.cambiarEstado(1L, "EN_CURSO"))
                .expectError(ReglaNegocioException.class)
                .verify();
    }
}
