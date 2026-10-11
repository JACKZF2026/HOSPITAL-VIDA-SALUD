package pe.cibertec.vidasalud.atenciones.controller;

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
import pe.cibertec.vidasalud.atenciones.dto.AtencionRequest;
import pe.cibertec.vidasalud.atenciones.dto.EstadoRequest;
import pe.cibertec.vidasalud.atenciones.model.Atencion;
import pe.cibertec.vidasalud.atenciones.service.AtencionService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/atenciones")
public class AtencionController {

    private final AtencionService service;

    public AtencionController(AtencionService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<Atencion>> registrar(@Valid @RequestBody AtencionRequest request) {
        return service.registrar(request)
                .map(a -> ResponseEntity.status(HttpStatus.CREATED).body(a));
    }

    @GetMapping
    public Flux<Atencion> listar(@RequestParam(required = false) Long pacienteId,
                               @RequestParam(required = false) String estado) {
        return service.listar(pacienteId, estado);
    }

    @GetMapping("/{id}")
    public Mono<Atencion> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/estado")
    public Mono<Atencion> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
