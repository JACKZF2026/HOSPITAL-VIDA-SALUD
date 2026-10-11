package pe.cibertec.vidasalud.notificaciones.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.cibertec.vidasalud.notificaciones.model.Notificacion;
import pe.cibertec.vidasalud.notificaciones.repository.NotificacionRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionRepository repository;

    public NotificacionController(NotificacionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Flux<Notificacion> listar(@RequestParam(required = false) String tipoEvento) {
        if (tipoEvento != null && !tipoEvento.isBlank()) {
            return repository.findByTipoEvento(tipoEvento);
        }
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Notificacion>> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
