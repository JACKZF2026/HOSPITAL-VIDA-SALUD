package pe.cibertec.vidasalud.laboratorio.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builder balanceado: las URIs http://<nombre-servicio> se resuelven con
 * Eureka + Spring Cloud LoadBalancer sin conocer host ni puerto.
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
