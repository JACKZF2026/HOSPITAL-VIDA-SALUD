package pe.cibertec.vidasalud.pagos.events;

import java.time.Instant;
import java.util.Map;

public record EventoIntegracion(
        String eventoId,
        String tipoEvento,
        String origen,
        Instant fechaEvento,
        Map<String, Object> datos
) {
}
