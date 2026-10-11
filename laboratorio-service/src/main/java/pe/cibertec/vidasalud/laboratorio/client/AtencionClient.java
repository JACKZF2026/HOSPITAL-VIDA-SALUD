package pe.cibertec.vidasalud.laboratorio.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.cibertec.vidasalud.laboratorio.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.laboratorio.exception.ServicioExternoException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Comunicacion sincrona con atenciones-service para validar la referencia
 * antes de registrar una solicitud de laboratorio.
 */
@Component
public class AtencionClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final WebClient webClient;

    public AtencionClient(@LoadBalanced WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://atenciones-service").build();
    }

    public Mono<AtencionResumen> obtenerAtencion(Long atencionId) {
        return webClient.get()
                .uri("/atenciones/{id}", atencionId)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, resp ->
                        Mono.error(new RecursoNoEncontradoException("Atencion no encontrada: " + atencionId)))
                .bodyToMono(AtencionResumen.class)
                .timeout(TIMEOUT)
                .onErrorMap(e -> !(e instanceof RecursoNoEncontradoException),
                        e -> new ServicioExternoException(
                                "atenciones-service no disponible o tiempo de espera agotado"));
    }
}
