package pe.cibertec.vidasalud.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro global que registra cada solicitud que atraviesa el Gateway,
 * util para evidenciar el enrutamiento en los logs.
 */
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String metodo = exchange.getRequest().getMethod().name();
        String ruta = exchange.getRequest().getPath().value();
        return chain.filter(exchange).doOnSuccess(done ->
                log.info("[GATEWAY] {} {} -> {}", metodo, ruta,
                        exchange.getResponse().getStatusCode()));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
