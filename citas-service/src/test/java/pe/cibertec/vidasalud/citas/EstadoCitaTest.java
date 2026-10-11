package pe.cibertec.vidasalud.citas;

import org.junit.jupiter.api.Test;
import pe.cibertec.vidasalud.citas.model.EstadoCita;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoCitaTest {

    @Test
    void transicionesPermitidas() {
        assertTrue(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.CONFIRMADA));
        assertTrue(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.CANCELADA));
        assertTrue(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.ATENDIDA));
        assertTrue(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.AUSENTE));
    }

    @Test
    void transicionesNoPermitidas() {
        // una cita cancelada no puede confirmarse
        assertFalse(EstadoCita.CANCELADA.puedeTransicionarA(EstadoCita.CONFIRMADA));
        // una cita pendiente no puede marcarse como atendida directamente
        assertFalse(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.ATENDIDA));
        // estados terminales no tienen salida
        assertFalse(EstadoCita.ATENDIDA.puedeTransicionarA(EstadoCita.PENDIENTE));
        assertFalse(EstadoCita.AUSENTE.puedeTransicionarA(EstadoCita.CONFIRMADA));
    }
}
