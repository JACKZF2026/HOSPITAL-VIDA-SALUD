package pe.cibertec.vidasalud.pagos.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.cibertec.vidasalud.pagos.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.pagos.exception.ServicioExternoException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Comunicacion sincrona con pacientes-service para validar la referencia
 * del paciente antes de registrar un pago simulado.
 */
@Component
public class PacienteClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final WebClient webClient;

    public PacienteClient(@LoadBalanced WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://pacientes-service").build();
    }

    public Mono<PacienteResumen> obtenerPaciente(Long pacienteId) {
        return webClient.get()
                .uri("/pacientes/{id}", pacienteId)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, resp ->
                        Mono.error(new RecursoNoEncontradoException("Paciente no encontrado: " + pacienteId)))
                .bodyToMono(PacienteResumen.class)
                .timeout(TIMEOUT)
                .onErrorMap(e -> !(e instanceof RecursoNoEncontradoException),
                        e -> new ServicioExternoException(
                                "pacientes-service no disponible o tiempo de espera agotado"));
    }
}
