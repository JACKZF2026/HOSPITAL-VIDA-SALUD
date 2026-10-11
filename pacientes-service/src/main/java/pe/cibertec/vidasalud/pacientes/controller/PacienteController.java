package pe.cibertec.vidasalud.pacientes.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cibertec.vidasalud.pacientes.dto.PacienteRequest;
import pe.cibertec.vidasalud.pacientes.model.Paciente;
import pe.cibertec.vidasalud.pacientes.service.PacienteService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService service;

    public PacienteController(PacienteService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<Paciente>> registrar(@Valid @RequestBody PacienteRequest request) {
        return service.registrar(request)
                .map(p -> ResponseEntity.status(HttpStatus.CREATED).body(p));
    }

    @GetMapping
    public Flux<Paciente> listar(@RequestParam(required = false) String estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    public Mono<Paciente> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/documento/{tipo}/{numero}")
    public Mono<Paciente> buscarPorDocumento(@PathVariable String tipo, @PathVariable String numero) {
        return service.buscarPorDocumento(tipo, numero);
    }

    @PutMapping("/{id}")
    public Mono<Paciente> actualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public Mono<Paciente> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return service.cambiarEstado(id, estado);
    }
}
