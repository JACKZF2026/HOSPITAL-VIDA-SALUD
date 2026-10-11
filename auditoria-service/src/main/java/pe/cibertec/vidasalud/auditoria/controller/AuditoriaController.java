package pe.cibertec.vidasalud.auditoria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cibertec.vidasalud.auditoria.model.EventoAuditoria;
import pe.cibertec.vidasalud.auditoria.repository.EventoAuditoriaRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auditoria")
public class AuditoriaController {

    private final EventoAuditoriaRepository repository;

    public AuditoriaController(EventoAuditoriaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Flux<EventoAuditoria> listar(@RequestParam(required = false) String tipoEvento,
                                        @RequestParam(required = false) String recursoId) {
        if (tipoEvento != null && !tipoEvento.isBlank()) {
            return repository.findByTipoEvento(tipoEvento);
        }
        if (recursoId != null && !recursoId.isBlank()) {
            return repository.findByRecursoId(recursoId);
        }
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<EventoAuditoria>> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
