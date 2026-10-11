package pe.cibertec.vidasalud.citas.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Maquina de estados de la cita. Las transiciones permitidas son:
 * PENDIENTE  -> CONFIRMADA, CANCELADA
 * CONFIRMADA -> ATENDIDA, CANCELADA, AUSENTE
 * ATENDIDA, CANCELADA, AUSENTE -> estados terminales
 */
public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    ATENDIDA,
    CANCELADA,
    AUSENTE;

    private static final Map<EstadoCita, Set<EstadoCita>> TRANSICIONES = new EnumMap<>(EstadoCita.class);

    static {
        TRANSICIONES.put(PENDIENTE, EnumSet.of(CONFIRMADA, CANCELADA));
        TRANSICIONES.put(CONFIRMADA, EnumSet.of(ATENDIDA, CANCELADA, AUSENTE));
        TRANSICIONES.put(ATENDIDA, EnumSet.noneOf(EstadoCita.class));
        TRANSICIONES.put(CANCELADA, EnumSet.of(CONFIRMADA));
        TRANSICIONES.put(AUSENTE, EnumSet.noneOf(EstadoCita.class));
    }

    public boolean puedeTransicionarA(EstadoCita destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public Set<EstadoCita> transicionesPermitidas() {
        return TRANSICIONES.get(this);
    }

    public static EstadoCita desdeTexto(String texto) {
        try {
            return EstadoCita.valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Estado de cita invalido: " + texto);
        }
    }
}
