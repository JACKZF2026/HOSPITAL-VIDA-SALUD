package pe.cibertec.vidasalud.laboratorio.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String error,
        String mensaje,
        String path,
        Instant timestamp,
        Map<String, String> detalles
) {
    public static ApiError of(int status, String error, String mensaje, String path) {
        return new ApiError(status, error, mensaje, path, Instant.now(), null);
    }
}
