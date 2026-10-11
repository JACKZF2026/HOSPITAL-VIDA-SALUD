package pe.cibertec.vidasalud.laboratorio.controller;

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
import pe.cibertec.vidasalud.laboratorio.dto.EstadoRequest;
import pe.cibertec.vidasalud.laboratorio.dto.ResultadoRequest;
import pe.cibertec.vidasalud.laboratorio.dto.SolicitudRequest;
import pe.cibertec.vidasalud.laboratorio.model.SolicitudLaboratorio;
import pe.cibertec.vidasalud.laboratorio.service.SolicitudLaboratorioService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/laboratorio")
public class SolicitudLaboratorioController {

    private final SolicitudLaboratorioService service;

    public SolicitudLaboratorioController(SolicitudLaboratorioService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<SolicitudLaboratorio>> registrar(@Valid @RequestBody SolicitudRequest request) {
        return service.registrar(request)
                .map(s -> ResponseEntity.status(HttpStatus.CREATED).body(s));
    }

    @GetMapping
    public Flux<SolicitudLaboratorio> listar(@RequestParam(required = false) Long pacienteId,
                                           @RequestParam(required = false) Long atencionId,
                                           @RequestParam(required = false) String estado) {
        return service.listar(pacienteId, atencionId, estado);
    }

    @GetMapping("/{id}")
    public Mono<SolicitudLaboratorio> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/estado")
    public Mono<SolicitudLaboratorio> cambiarEstado(@PathVariable Long id,
                                                    @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @PatchMapping("/{id}/resultado")
    public Mono<SolicitudLaboratorio> registrarResultado(@PathVariable Long id,
                                                         @Valid @RequestBody ResultadoRequest request) {
        return service.registrarResultado(id, request.resultado());
    }
}
