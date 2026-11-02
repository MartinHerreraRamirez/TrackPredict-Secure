package com.trackpredict;

import com.trackpredict.repository.GeoRepository.Punto;
import com.trackpredict.service.AnalyticsClient.ObservacionIn;
import com.trackpredict.service.ReentrenamientoService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReentrenamientoServiceTest {
    @Test
    void viajeValidoSeConvierteEnObservacionConHoraYDiaLocales() {
        // 2026-10-14 (miercoles) 13:00 UTC = 10:00 en Buenos Aires; 30 minutos para 15 km = 30 km/h
        Instant ini = Instant.parse("2026-10-14T13:00:00Z");
        Optional<ObservacionIn> o = ReentrenamientoService.aObservacion(ini, ini.plusSeconds(1800), 15.0);
        assertTrue(o.isPresent());
        assertEquals(30.0, o.get().minutos_reales(), 1e-9);
        assertEquals(30.0, o.get().velocidad_media_kmh(), 1e-9);
        assertEquals(10, o.get().hora());
        assertEquals(2, o.get().dia_semana());
    }

    @Test
    void seDescartanViajesImplausibles() {
        Instant ini = Instant.parse("2026-10-14T13:00:00Z");
        assertTrue(ReentrenamientoService.aObservacion(ini, ini.plusSeconds(20), 15.0).isEmpty());      // cerrado al instante
        assertTrue(ReentrenamientoService.aObservacion(ini, ini.plusSeconds(60), 15.0).isEmpty());      // 900 km/h
        assertTrue(ReentrenamientoService.aObservacion(ini, null, 15.0).isEmpty());                     // sin fin real
        assertTrue(ReentrenamientoService.aObservacion(ini, ini.plusSeconds(1800), 0).isEmpty());       // ruta sin largo
    }

    @Test
    void largoDeRutaEnKilometros() {
        // un grado de latitud son unos 111 km
        double km = ReentrenamientoService.largoKm(List.of(new Punto(-34.0, -58.0), new Punto(-35.0, -58.0)));
        assertEquals(111.2, km, 0.5);
    }
}
