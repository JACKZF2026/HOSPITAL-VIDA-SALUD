package pe.cibertec.vidasalud.pacientes;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Prueba de integracion: flujo completo de pacientes contra H2 real
 * mediante R2DBC. RabbitTemplate se reemplaza por un mock para no
 * requerir un broker y poder verificar la publicacion del evento.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.rabbitmq.dynamic=false"
        })
class PacienteIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    private static final String PACIENTE_JSON = """
            {"tipoDocumento":"DNI","numeroDocumento":"99887766",
             "nombres":"Maria","apellidos":"Lopez Prueba","fechaNacimiento":"1995-03-20",
             "telefono":"999888777","correo":"maria.prueba@example.com"}
            """;

    @Test
    void flujoCompletoRegistroConsultaYRechazoDuplicado() {
        // 1. Registrar paciente -> 201
        webTestClient.post().uri("/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(PACIENTE_JSON)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.estado").isEqualTo("ACTIVO")
                .jsonPath("$.tipoDocumento").isEqualTo("DNI");

        // 2. El evento paciente.registrado se publico en el exchange
        verify(rabbitTemplate, timeout(2000)).convertAndSend(
                eq("hospital.eventos"), eq("paciente.registrado"), any(Object.class));

        // 3. Consultar el paciente registrado -> 200
        webTestClient.get().uri("/pacientes/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numeroDocumento").isEqualTo("99887766");

        // 4. Intentar registrar el mismo documento -> 409
        webTestClient.post().uri("/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(PACIENTE_JSON)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("Ya existe un paciente con DNI 99887766");

        // 5. Recurso inexistente -> 404
        webTestClient.get().uri("/pacientes/777")
                .exchange()
                .expectStatus().isNotFound();
    }
}
