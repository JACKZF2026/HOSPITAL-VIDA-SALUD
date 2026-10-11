package pe.cibertec.vidasalud.citas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cibertec.vidasalud.citas.dto.CitaRequest;
import pe.cibertec.vidasalud.citas.dto.EstadoRequest;
import pe.cibertec.vidasalud.citas.dto.ReprogramarRequest;
import pe.cibertec.vidasalud.citas.model.Cita;
import pe.cibertec.vidasalud.citas.service.CitaService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/citas")
public class CitaController {

    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<Cita>> programar(@Valid @RequestBody CitaRequest request) {
        return service.programar(request)
                .map(c -> ResponseEntity.status(HttpStatus.CREATED).body(c));
    }

    @GetMapping
    public Flux<Cita> listar(@RequestParam(required = false) Long pacienteId,
                             @RequestParam(required = false) String estado) {
        return service.listar(pacienteId, estado);
    }

    @GetMapping("/{id}")
    public Mono<Cita> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/estado")
    public Mono<Cita> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @PatchMapping("/{id}/reprogramar")
    public Mono<Cita> reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarRequest request) {
        return service.reprogramar(id, request.fechaHora());
    }
}
