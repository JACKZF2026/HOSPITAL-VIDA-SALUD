package pe.cibertec.vidasalud.pagos.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public Mono<ResponseEntity<ApiError>> noEncontrado(RecursoNoEncontradoException ex, ServerWebExchange exchange) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public Mono<ResponseEntity<ApiError>> reglaNegocio(ReglaNegocioException ex, ServerWebExchange exchange) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    @ExceptionHandler(ServicioExternoException.class)
    public Mono<ResponseEntity<ApiError>> servicioExterno(ServicioExternoException ex, ServerWebExchange exchange) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiError>> validacion(WebExchangeBindException ex, ServerWebExchange exchange) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getFieldErrors().forEach(fe -> detalles.put(fe.getField(), fe.getDefaultMessage()));
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(), "Solicitud invalida",
                "Error de validacion en los campos enviados",
                exchange.getRequest().getPath().value(), java.time.Instant.now(), detalles);
        return Mono.just(ResponseEntity.badRequest().body(apiError));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ApiError>> entradaInvalida(ServerWebInputException ex, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, "Cuerpo o parametros de solicitud invalidos", exchange);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Mono<ResponseEntity<ApiError>> claveDuplicada(DuplicateKeyException ex, ServerWebExchange exchange) {
        return error(HttpStatus.CONFLICT, "Ya existe un registro con los mismos datos unicos", exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiError>> generico(Exception ex, ServerWebExchange exchange) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servicio", exchange);
    }

    private Mono<ResponseEntity<ApiError>> error(HttpStatus status, String mensaje, ServerWebExchange exchange) {
        ApiError apiError = ApiError.of(status.value(), status.getReasonPhrase(), mensaje,
                exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(status).body(apiError));
    }
}
