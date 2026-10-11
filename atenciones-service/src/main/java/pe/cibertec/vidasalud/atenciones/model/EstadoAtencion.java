package pe.cibertec.vidasalud.atenciones.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Maquina de estados de la atencion administrativa:
 * REGISTRADA -> EN_CURSO, CANCELADA
 * EN_CURSO   -> FINALIZADA, CANCELADA
 * FINALIZADA, CANCELADA -> terminales
 */
public enum EstadoAtencion {
    REGISTRADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA;

    private static final Map<EstadoAtencion, Set<EstadoAtencion>> TRANSICIONES = new EnumMap<>(EstadoAtencion.class);

    static {
        TRANSICIONES.put(REGISTRADA, EnumSet.of(EN_CURSO, CANCELADA));
        TRANSICIONES.put(EN_CURSO, EnumSet.of(FINALIZADA, CANCELADA));
        TRANSICIONES.put(FINALIZADA, EnumSet.noneOf(EstadoAtencion.class));
        TRANSICIONES.put(CANCELADA, EnumSet.noneOf(EstadoAtencion.class));
    }

    public boolean puedeTransicionarA(EstadoAtencion destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public Set<EstadoAtencion> transicionesPermitidas() {
        return TRANSICIONES.get(this);
    }

    public static EstadoAtencion desdeTexto(String texto) {
        try {
            return EstadoAtencion.valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Estado de atencion invalido: " + texto);
        }
    }
}
