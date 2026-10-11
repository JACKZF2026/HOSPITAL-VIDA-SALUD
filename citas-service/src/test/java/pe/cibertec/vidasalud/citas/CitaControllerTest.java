package pe.cibertec.vidasalud.citas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.cibertec.vidasalud.citas.controller.CitaController;
import pe.cibertec.vidasalud.citas.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.citas.model.Cita;
import pe.cibertec.vidasalud.citas.service.CitaService;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(CitaController.class)
class CitaControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    CitaService service;

    @Test
    void transicionInvalidaDevuelve409() {
        when(service.cambiarEstado(eq(1L), eq("CONFIRMADA")))
                .thenReturn(Mono.error(new ReglaNegocioException(
                        "Transicion no permitida: CANCELADA -> CONFIRMADA")));

        webTestClient.patch().uri("/citas/1/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\":\"CONFIRMADA\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("Transicion no permitida: CANCELADA -> CONFIRMADA");
    }

    @Test
    void fechaPasadaDevuelve400() {
        webTestClient.post().uri("/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"pacienteId":1,"especialidad":"Cardiologia",
                         "fechaHora":"2020-01-01T10:00:00","profesionalCodigo":"PRO-1"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void cambioEstadoValidoDevuelve200() {
        Cita c = new Cita();
        c.setId(1L);
        c.setEstado("CONFIRMADA");
        c.setFechaHora(LocalDateTime.now().plusDays(1));
        when(service.cambiarEstado(1L, "CONFIRMADA")).thenReturn(Mono.just(c));

        webTestClient.patch().uri("/citas/1/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\":\"CONFIRMADA\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("CONFIRMADA");
    }
}
