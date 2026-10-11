package pe.cibertec.vidasalud.pagos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.cibertec.vidasalud.pagos.client.PacienteClient;
import pe.cibertec.vidasalud.pagos.client.PacienteResumen;
import pe.cibertec.vidasalud.pagos.dto.PagoRequest;
import pe.cibertec.vidasalud.pagos.events.EventoPublicador;
import pe.cibertec.vidasalud.pagos.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.pagos.model.EstadoPago;
import pe.cibertec.vidasalud.pagos.model.Pago;
import pe.cibertec.vidasalud.pagos.repository.PagoRepository;
import pe.cibertec.vidasalud.pagos.service.PagoService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    PagoRepository repository;

    @Mock
    PacienteClient pacienteClient;

    @Mock
    EventoPublicador eventoPublicador;

    @InjectMocks
    PagoService service;

    private PagoRequest request(BigDecimal monto) {
        return new PagoRequest(1L, "Consulta externa", monto, "YAPE", "OP-TEST-001");
    }

    @Test
    void montoCeroONegativoEsRechazado() {
        StepVerifier.create(service.registrar(request(BigDecimal.ZERO)))
                .expectErrorMatches(e -> e instanceof ReglaNegocioException
                        && e.getMessage().contains("mayor que cero"))
                .verify();

        StepVerifier.create(service.registrar(request(new BigDecimal("-50"))))
                .expectError(ReglaNegocioException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void numeroOperacionDuplicadoEsRechazado() {
        when(repository.existsByNumeroOperacion("OP-TEST-001")).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(request(new BigDecimal("120.50"))))
                .expectErrorMatches(e -> e instanceof ReglaNegocioException
                        && e.getMessage().contains("numero de operacion"))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void pagoValidoSeRegistraYPublicaEvento() {
        when(repository.existsByNumeroOperacion("OP-TEST-001")).thenReturn(Mono.just(false));
        when(pacienteClient.obtenerPaciente(1L))
                .thenReturn(Mono.just(new PacienteResumen(1L, "Ana", "Ruiz", "ACTIVO")));
        when(repository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(8L);
            return Mono.just(p);
        });

        StepVerifier.create(service.registrar(request(new BigDecimal("120.50"))))
                .expectNextMatches(p -> p.getId() == 8L
                        && EstadoPago.REGISTRADO.name().equals(p.getEstado()))
                .verifyComplete();

        verify(eventoPublicador).publicar(eq("pago.registrado"), anyMap());
    }

    @Test
    void anularPagoAnuladoEsRechazado() {
        Pago p = new Pago();
        p.setId(1L);
        p.setEstado(EstadoPago.ANULADO.name());
        when(repository.findById(1L)).thenReturn(Mono.just(p));

        StepVerifier.create(service.cambiarEstado(1L, "ANULADO"))
                .expectError(ReglaNegocioException.class)
                .verify();
    }
}
