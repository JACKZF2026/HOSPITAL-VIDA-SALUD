package pe.cibertec.vidasalud.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rutas del API Gateway.
 * El prefijo /api se elimina con stripPrefix(1) y la URI lb:// resuelve la
 * instancia real mediante Eureka + Spring Cloud LoadBalancer, de modo que los
 * clientes nunca conocen los puertos internos de cada microservicio.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator hospitalRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("pacientes-route", r -> r
                        .path("/api/pacientes/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://pacientes-service"))
                .route("citas-route", r -> r
                        .path("/api/citas/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://citas-service"))
                .route("atenciones-route", r -> r
                        .path("/api/atenciones/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://atenciones-service"))
                .route("laboratorio-route", r -> r
                        .path("/api/laboratorio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://laboratorio-service"))
                .route("pagos-route", r -> r
                        .path("/api/pagos/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://pagos-service"))
                .route("notificaciones-route", r -> r
                        .path("/api/notificaciones/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://notificaciones-service"))
                .route("auditoria-route", r -> r
                        .path("/api/auditoria/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://auditoria-service"))
                .build();
    }
}
