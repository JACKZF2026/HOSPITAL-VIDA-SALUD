package pe.cibertec.vidasalud.laboratorio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.cibertec.vidasalud.laboratorio.client.AtencionClient;
import pe.cibertec.vidasalud.laboratorio.client.AtencionResumen;
import pe.cibertec.vidasalud.laboratorio.dto.SolicitudRequest;
import pe.cibertec.vidasalud.laboratorio.events.EventoPublicador;
import pe.cibertec.vidasalud.laboratorio.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.laboratorio.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.laboratorio.model.EstadoSolicitud;
import pe.cibertec.vidasalud.laboratorio.model.SolicitudLaboratorio;
import pe.cibertec.vidasalud.laboratorio.repository.SolicitudLaboratorioRepository;
import pe.cibertec.vidasalud.laboratorio.service.SolicitudLaboratorioService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudLaboratorioServiceTest {

    @Mock
    SolicitudLaboratorioRepository repository;

    @Mock
    AtencionClient atencionClient;

    @Mock
    EventoPublicador eventoPublicador;

    @InjectMocks
    SolicitudLaboratorioService service;

    @Test
    void registrarSolicitudConAtencionValidaGuardaYPublica() {
        when(atencionClient.obtenerAtencion(5L))
                .thenReturn(Mono.just(new AtencionResumen(5L, "ATE-1", 2L, 9L, "EN_CURSO")));
        when(repository.save(any(SolicitudLaboratorio.class))).thenAnswer(inv -> {
            SolicitudLaboratorio s = inv.getArgument(0);
            s.setId(1L);
            return Mono.just(s);
        });

        StepVerifier.create(service.registrar(new SolicitudRequest(5L, "Hemograma de prueba")))
                .expectNextMatches(s -> s.getPacienteId() == 9L
                        && EstadoSolicitud.SOLICITADO.name().equals(s.getEstado()))
                .verifyComplete();

        verify(eventoPublicador).publicar(eq("laboratorio.actualizado"), anyMap());
    }

    @Test
    void registrarSolicitudConAtencionInexistenteEsRechazada() {
        when(atencionClient.obtenerAtencion(99L))
                .thenReturn(Mono.error(new RecursoNoEncontradoException("Atencion no encontrada: 99")));

        StepVerifier.create(service.registrar(new SolicitudRequest(99L, "Hemograma")))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
    }

    @Test
    void registrarResultadoSinEstarEnProcesoEsRechazado() {
        SolicitudLaboratorio s = new SolicitudLaboratorio();
        s.setId(1L);
        s.setEstado(EstadoSolicitud.SOLICITADO.name());
        when(repository.findById(1L)).thenReturn(Mono.just(s));

        StepVerifier.create(service.registrarResultado(1L, "Resultado ficticio"))
                .expectError(ReglaNegocioException.class)
                .verify();
    }

    @Test
    void registrarResultadoEnProcesoCompletaSolicitud() {
        SolicitudLaboratorio s = new SolicitudLaboratorio();
        s.setId(1L);
        s.setCodigo("LAB-1");
        s.setEstado(EstadoSolicitud.EN_PROCESO.name());
        when(repository.findById(1L)).thenReturn(Mono.just(s));
        when(repository.save(any(SolicitudLaboratorio.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.registrarResultado(1L, "Resultado ficticio"))
                .expectNextMatches(r -> EstadoSolicitud.COMPLETADO.name().equals(r.getEstado())
                        && "Resultado ficticio".equals(r.getResultadoFicticio()))
                .verifyComplete();
    }
}
