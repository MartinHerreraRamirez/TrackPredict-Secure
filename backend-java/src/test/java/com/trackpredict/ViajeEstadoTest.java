package com.trackpredict;

import com.trackpredict.domain.ViajeEstado;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ViajeEstadoTest {
    @Test
    void cicloDeVidaNormal() {
        assertTrue(ViajeEstado.transicionValida("PLANIFICADO", "EN_CURSO"));
        assertTrue(ViajeEstado.transicionValida("EN_CURSO", "FINALIZADO"));
    }

    @Test
    void sePuedeCancelarAntesDeFinalizar() {
        assertTrue(ViajeEstado.transicionValida("PLANIFICADO", "CANCELADO"));
        assertTrue(ViajeEstado.transicionValida("EN_CURSO", "CANCELADO"));
    }

    @Test
    void transicionesInvalidas() {
        assertFalse(ViajeEstado.transicionValida("PLANIFICADO", "FINALIZADO"));   // no se puede saltar EN_CURSO
        assertFalse(ViajeEstado.transicionValida("FINALIZADO", "EN_CURSO"));      // estados finales
        assertFalse(ViajeEstado.transicionValida("CANCELADO", "PLANIFICADO"));
    }
}
