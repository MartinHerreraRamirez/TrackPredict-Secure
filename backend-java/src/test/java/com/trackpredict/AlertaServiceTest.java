package com.trackpredict;

import com.trackpredict.domain.Posicion;
import com.trackpredict.service.AlertaService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertaServiceTest {
    private static List<Posicion> serie(double velocidad, int n, int pasoSeg) {
        Instant fin = Instant.parse("2026-10-12T10:00:00Z");
        List<Posicion> l = new ArrayList<>();   // de la mas nueva a la mas vieja
        for (int i = 0; i < n; i++) l.add(new Posicion(1L, 1L, fin.minusSeconds((long) i * pasoSeg), -34.6, -58.4, velocidad));
        return l;
    }

    @Test
    void excesoDeVelocidad() {
        assertTrue(AlertaService.excedeVelocidad(95, 80));
        assertFalse(AlertaService.excedeVelocidad(80, 80));
    }

    @Test
    void paradaProlongadaSoloSiCubreLaVentana() {
        assertTrue(AlertaService.paradaProlongada(serie(0, 12, 30), Duration.ofMinutes(5)));   // 5,5 min detenido
        assertFalse(AlertaService.paradaProlongada(serie(0, 5, 30), Duration.ofMinutes(5)));   // solo 2 min
    }

    @Test
    void siSeMovioNoHayParada() {
        List<Posicion> l = serie(0, 12, 30);
        l.set(3, new Posicion(1L, 1L, l.get(3).getTs(), -34.6, -58.4, 40));
        assertFalse(AlertaService.paradaProlongada(l, Duration.ofMinutes(5)));
    }

    @Test
    void unaAlertaReconocidaNoSeVuelveACrearMientrasSigaAbierta() {
        assertFalse(AlertaService.debeCrear(true, false));    // hay una abierta (por ejemplo, reconocida): no se duplica
        assertFalse(AlertaService.debeCrear(false, true));    // se cerro hace instantes: no reaparece de inmediato
        assertTrue(AlertaService.debeCrear(false, false));    // ya no hay nada abierto ni reciente: puede crearse otra
    }
}
