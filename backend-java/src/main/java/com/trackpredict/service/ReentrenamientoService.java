package com.trackpredict.service;

import com.trackpredict.domain.Viaje;
import com.trackpredict.domain.ViajeEstado;
import com.trackpredict.repository.GeoRepository;
import com.trackpredict.repository.GeoRepository.Punto;
import com.trackpredict.repository.ViajeRepository;
import com.trackpredict.service.AnalyticsClient.ObservacionIn;
import com.trackpredict.service.AnalyticsClient.ReentrenarRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Convierte los viajes FINALIZADOS de una empresa en observaciones reales (distancia, velocidad media, hora,
 * dia y duracion real) y le pide al servicio Python que reentrene el modelo de ETA con ellas.
 */
@Service
public class ReentrenamientoService {
    public static final int MINIMO = 20;                 // el mismo minimo que exige el servicio Python
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final double R = 6371000.0;

    private final ViajeRepository viajes;
    private final GeoRepository geo;
    private final AnalyticsClient analytics;

    public ReentrenamientoService(ViajeRepository viajes, GeoRepository geo, AnalyticsClient analytics) {
        this.viajes = viajes; this.geo = geo; this.analytics = analytics;
    }

    /** Observacion valida de un viaje, o vacio si sus datos no son plausibles (por ejemplo, un viaje cerrado al instante). */
    public static Optional<ObservacionIn> aObservacion(Instant inicio, Instant fin, double distanciaKm) {
        if (inicio == null || fin == null || distanciaKm <= 0) return Optional.empty();
        double minutos = Duration.between(inicio, fin).toSeconds() / 60.0;
        if (minutos < 1 || minutos > 2000) return Optional.empty();
        double velocidad = distanciaKm / (minutos / 60.0);
        if (velocidad <= 0 || velocidad > 200) return Optional.empty();
        ZonedDateTime t = inicio.atZone(ZONA);
        return Optional.of(new ObservacionIn(distanciaKm, velocidad, t.getHour(), t.getDayOfWeek().getValue() - 1, minutos));
    }

    public static double largoKm(List<Punto> puntos) {
        double total = 0;
        for (int i = 1; i < puntos.size(); i++) total += haversineM(puntos.get(i - 1), puntos.get(i));
        return total / 1000.0;
    }

    private static double haversineM(Punto a, Punto b) {
        double p1 = Math.toRadians(a.lat()), p2 = Math.toRadians(b.lat());
        double dphi = p2 - p1, dl = Math.toRadians(b.lon() - a.lon());
        double h = Math.sin(dphi / 2) * Math.sin(dphi / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * R * Math.asin(Math.sqrt(h));
    }

    public List<ObservacionIn> observaciones(Long empresaId) {
        List<ObservacionIn> out = new ArrayList<>();
        for (Viaje v : viajes.findByEmpresaIdAndEstadoOrderByIdDesc(empresaId, ViajeEstado.FINALIZADO)) {
            double km = largoKm(geo.puntosRuta(v.getRutaId(), empresaId));
            aObservacion(v.getInicioReal(), v.getFinReal(), km).ifPresent(out::add);
        }
        return out;
    }

    public Map<String, Object> reentrenar(Long empresaId) {
        List<ObservacionIn> obs = observaciones(empresaId);
        if (obs.size() < MINIMO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Se necesitan al menos " + MINIMO + " viajes finalizados utilizables y hay " + obs.size());
        }
        return analytics.reentrenar(new ReentrenarRequest(obs));
    }
}
