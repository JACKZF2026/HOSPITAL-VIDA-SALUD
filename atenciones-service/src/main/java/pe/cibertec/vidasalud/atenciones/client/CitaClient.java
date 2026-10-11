package pe.cibertec.vidasalud.atenciones.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.cibertec.vidasalud.atenciones.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.atenciones.exception.ServicioExternoException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Comunicacion sincrona con citas-service:
 *  - GET  /citas/{id}        valida que la cita exista
 *  - PATCH /citas/{id}/estado marca la cita como ATENDIDA
 * La URI se resuelve por Eureka mediante lb:// (LoadBalancer).
 */
@Component
public class CitaClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final WebClient webClient;

    public CitaClient(@LoadBalanced WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://citas-service").build();
    }

    public Mono<CitaResumen> obtenerCita(Long citaId) {
        return webClient.get()
                .uri("/citas/{id}", citaId)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, resp ->
                        Mono.error(new RecursoNoEncontradoException("Cita no encontrada: " + citaId)))
                .bodyToMono(CitaResumen.class)
                .timeout(TIMEOUT)
                .onErrorMap(e -> !(e instanceof RecursoNoEncontradoException),
                        e -> new ServicioExternoException(
                                "citas-service no disponible o tiempo de espera agotado"));
    }

    public Mono<Void> marcarComoAtendida(Long citaId) {
        return webClient.patch()
                .uri("/citas/{id}/estado", citaId)
                .bodyValue(Map.of("estado", "ATENDIDA"))
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(TIMEOUT)
                .onErrorMap(e -> !(e instanceof RecursoNoEncontradoException),
                        e -> new ServicioExternoException(
                                "citas-service no disponible al actualizar estado de la cita"))
                .then();
    }
}
