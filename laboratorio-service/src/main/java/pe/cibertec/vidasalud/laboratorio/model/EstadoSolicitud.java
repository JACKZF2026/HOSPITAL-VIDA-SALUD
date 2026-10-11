package pe.cibertec.vidasalud.laboratorio.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Maquina de estados de la solicitud de laboratorio:
 * SOLICITADO -> EN_PROCESO, CANCELADO
 * EN_PROCESO -> COMPLETADO, CANCELADO
 * COMPLETADO, CANCELADO -> terminales
 */
public enum EstadoSolicitud {
    SOLICITADO,
    EN_PROCESO,
    COMPLETADO,
    CANCELADO;

    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES = new EnumMap<>(EstadoSolicitud.class);

    static {
        TRANSICIONES.put(SOLICITADO, EnumSet.of(EN_PROCESO, CANCELADO));
        TRANSICIONES.put(EN_PROCESO, EnumSet.of(COMPLETADO, CANCELADO));
        TRANSICIONES.put(COMPLETADO, EnumSet.noneOf(EstadoSolicitud.class));
        TRANSICIONES.put(CANCELADO, EnumSet.noneOf(EstadoSolicitud.class));
    }

    public boolean puedeTransicionarA(EstadoSolicitud destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public Set<EstadoSolicitud> transicionesPermitidas() {
        return TRANSICIONES.get(this);
    }

    public static EstadoSolicitud desdeTexto(String texto) {
        try {
            return EstadoSolicitud.valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Estado de solicitud invalido: " + texto);
        }
    }
}
