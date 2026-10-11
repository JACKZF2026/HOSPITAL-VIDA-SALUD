package pe.cibertec.vidasalud.pagos.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Maquina de estados del pago simulado:
 * REGISTRADO -> ANULADO
 * ANULADO    -> terminal
 */
public enum EstadoPago {
    REGISTRADO,
    ANULADO;

    private static final Map<EstadoPago, Set<EstadoPago>> TRANSICIONES = new EnumMap<>(EstadoPago.class);

    static {
        TRANSICIONES.put(REGISTRADO, EnumSet.of(ANULADO));
        TRANSICIONES.put(ANULADO, EnumSet.noneOf(EstadoPago.class));
    }

    public boolean puedeTransicionarA(EstadoPago destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public Set<EstadoPago> transicionesPermitidas() {
        return TRANSICIONES.get(this);
    }

    public static EstadoPago desdeTexto(String texto) {
        try {
            return EstadoPago.valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Estado de pago invalido: " + texto);
        }
    }
}
