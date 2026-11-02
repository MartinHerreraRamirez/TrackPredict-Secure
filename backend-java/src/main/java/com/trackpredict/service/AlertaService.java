package com.trackpredict.service;

import com.trackpredict.domain.Alerta;
import com.trackpredict.domain.Posicion;
import com.trackpredict.repository.AlertaRepository;
import com.trackpredict.repository.GeoRepository;
import com.trackpredict.repository.PosicionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Reglas de alerta evaluadas en cada posicion ingestada. */
@Service
public class AlertaService {
    private static final double UMBRAL_DETENIDO_KMH = 3.0;
    private static final Duration ANTI_SPAM = Duration.ofMinutes(2);

    private final AlertaRepository alertas;
    private final PosicionRepository posiciones;
    private final GeoRepository geo;
    private final int limiteVelocidad;
    private final Duration paradaMax;

    public AlertaService(AlertaRepository alertas, PosicionRepository posiciones, GeoRepository geo,
                         @Value("${trackpredict.alertas.velocidad-limite-kmh}") int limiteVelocidad,
                         @Value("${trackpredict.alertas.parada-prolongada-min}") long paradaMin) {
        this.alertas = alertas;
        this.posiciones = posiciones;
        this.geo = geo;
        this.limiteVelocidad = limiteVelocidad;
        this.paradaMax = Duration.ofMinutes(paradaMin);
    }

    /** rutaId: ruta del viaje en curso del vehiculo (null si no hay viaje en curso). */
    public void evaluar(Posicion p, Long rutaId) {
        if (excedeVelocidad(p.getVelocidadKmh(), limiteVelocidad)) {
            crearSiNoExiste(p, Alerta.EXCESO_VELOCIDAD,
                    "Velocidad %.0f km/h supera el limite de %d km/h".formatted(p.getVelocidadKmh(), limiteVelocidad));
        }
        if (rutaId != null && geo.fueraDeGeocerca(p.getEmpresaId(), rutaId, p.getLat(), p.getLon())) {
            crearSiNoExiste(p, Alerta.SALIDA_GEOCERCA, "Vehiculo fuera del corredor autorizado de la ruta");
        }
        List<Posicion> recientes = posiciones.findByVehiculoIdAndEmpresaIdOrderByTsDesc(
                p.getVehiculoId(), p.getEmpresaId(), PageRequest.of(0, 20));
        if (paradaProlongada(recientes, paradaMax)) {
            crearSiNoExiste(p, Alerta.PARADA_PROLONGADA,
                    "Vehiculo detenido mas de " + paradaMax.toMinutes() + " minutos");
        }
    }

    public void crearSiNoExiste(Posicion p, String tipo, String detalle) {
        crearSiNoExiste(p.getEmpresaId(), p.getVehiculoId(), tipo, detalle);
    }

    public void crearSiNoExiste(Long empresaId, Long vehiculoId, String tipo, String detalle) {
        // No se repite mientras la alerta siga abierta (nueva, reconocida o en atencion), y tampoco durante un tiempo
        // despues de cerrarla. Antes solo se miraba "nueva": al reconocerla reaparecia otra igual en el acto.
        boolean abierta = alertas.existsByVehiculoIdAndTipoAndEstadoNot(vehiculoId, tipo, "CERRADA");
        boolean reciente = alertas.existsByVehiculoIdAndTipoAndCreadaEnAfter(vehiculoId, tipo, Instant.now().minus(ANTI_SPAM));
        if (debeCrear(abierta, reciente)) {
            alertas.save(new Alerta(empresaId, vehiculoId, tipo, detalle));
        }
    }

    /** Regla pura: se crea una alerta nueva solo si no hay otra abierta del mismo tipo ni una creada hace instantes. */
    public static boolean debeCrear(boolean hayAbierta, boolean hayRecienteCreada) {
        return !hayAbierta && !hayRecienteCreada;
    }

    // --- reglas puras (faciles de testear) ---
    public static boolean excedeVelocidad(double velocidadKmh, int limiteKmh) {
        return velocidadKmh > limiteKmh;
    }

    /** recientesDesc: posiciones ordenadas de la mas nueva a la mas vieja. */
    public static boolean paradaProlongada(List<Posicion> recientesDesc, Duration minimo) {
        if (recientesDesc.size() < 2) return false;
        Instant nueva = recientesDesc.get(0).getTs();
        Instant limite = nueva.minus(minimo);
        boolean cubreVentana = false;
        for (Posicion p : recientesDesc) {
            if (p.getVelocidadKmh() >= UMBRAL_DETENIDO_KMH) return false;
            if (!p.getTs().isAfter(limite)) cubreVentana = true;
        }
        return cubreVentana;
    }
}
