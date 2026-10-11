package pe.cibertec.vidasalud.pacientes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.cibertec.vidasalud.pacientes.controller.PacienteController;
import pe.cibertec.vidasalud.pacientes.exception.RecursoNoEncontradoException;
import pe.cibertec.vidasalud.pacientes.exception.ReglaNegocioException;
import pe.cibertec.vidasalud.pacientes.model.Paciente;
import pe.cibertec.vidasalud.pacientes.service.PacienteService;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(PacienteController.class)
class PacienteControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    PacienteService service;

    @Test
    void recursoInexistenteDevuelve404() {
        when(service.buscarPorId(99L))
                .thenReturn(Mono.error(new RecursoNoEncontradoException("Paciente no encontrado: 99")));

        webTestClient.get().uri("/pacientes/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.mensaje").isEqualTo("Paciente no encontrado: 99");
    }

    @Test
    void solicitudInvalidaDevuelve400() {
        webTestClient.post().uri("/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"tipoDocumento\":\"DNI\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detalles").exists();
    }

    @Test
    void duplicadoDevuelve409() {
        when(service.registrar(any()))
                .thenReturn(Mono.error(new ReglaNegocioException("Ya existe un paciente con DNI 12345678")));

        webTestClient.post().uri("/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"tipoDocumento":"DNI","numeroDocumento":"12345678",
                         "nombres":"Juan","apellidos":"Perez","fechaNacimiento":"1990-05-10"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409);
    }

    @Test
    void registroValidoDevuelve201() {
        Paciente p = new Paciente();
        p.setId(1L);
        when(service.registrar(any())).thenReturn(Mono.just(p));

        webTestClient.post().uri("/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"tipoDocumento":"DNI","numeroDocumento":"12345678",
                         "nombres":"Juan","apellidos":"Perez","fechaNacimiento":"1990-05-10"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
    }
}
