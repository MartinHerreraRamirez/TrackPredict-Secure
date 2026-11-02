package com.trackpredict;

import com.trackpredict.repository.GeoRepository;
import com.trackpredict.repository.GeoRepository.Punto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoWktTest {
    @Test
    void lineaUsaLongitudPrimero() {
        String wkt = GeoRepository.wktLinea(List.of(new Punto(-34.6037, -58.3816), new Punto(-34.6080, -58.3700)));
        assertEquals("LINESTRING(-58.381600 -34.603700,-58.370000 -34.608000)", wkt);
    }

    @Test
    void poligonoSeCierraSolo() {
        String wkt = GeoRepository.wktPoligono(List.of(new Punto(-34.60, -58.40), new Punto(-34.60, -58.38), new Punto(-34.62, -58.38)));
        assertTrue(wkt.startsWith("POLYGON(("));
        String[] pts = wkt.substring(9, wkt.length() - 2).split(",");
        assertEquals(4, pts.length);
        assertEquals(pts[0], pts[3]);
    }

    @Test
    void poligonoYaCerradoNoSeDuplica() {
        Punto a = new Punto(-34.60, -58.40);
        String wkt = GeoRepository.wktPoligono(List.of(a, new Punto(-34.60, -58.38), new Punto(-34.62, -58.38), a));
        assertEquals(4, wkt.substring(9, wkt.length() - 2).split(",").length);
    }
}
