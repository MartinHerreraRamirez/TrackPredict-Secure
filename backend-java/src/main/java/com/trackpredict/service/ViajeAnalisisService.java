package com.trackpredict.service;

import com.trackpredict.domain.Alerta;
import com.trackpredict.domain.Posicion;
import com.trackpredict.domain.Viaje;
import com.trackpredict.repository.GeoRepository;
import com.trackpredict.repository.GeoRepository.Punto;
import com.trackpredict.repository.PosicionRepository;
import com.trackpredict.service.AnalyticsClient.EtaRequest;
import com.trackpredict.service.AnalyticsClient.PuntoIn;
import com.trackpredict.service.AnalyticsClient.TrayectoRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Arma las entradas de los modelos de Python a partir de los datos reales del viaje. */
@Service
public class ViajeAnalisisService {
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final double UMBRAL_CRITICO = 70.0;

    private final AnalyticsClient analytics;
    private final PosicionRepository posiciones;
    private final GeoRepository geo;
    private final AlertaService alertas;

    public ViajeAnalisisService(AnalyticsClient analytics, PosicionRepository posiciones, GeoRepository geo, AlertaService alertas) {
        this.analytics = analytics; this.posiciones = posiciones; this.geo = geo; this.alertas = alertas;
    }

    /** ETA en vivo: distancia restante sobre la ruta, velocidad reciente y hora/dia actuales. */
    public Map<String, Object> eta(Viaje v) {
        List<Posicion> recientes = posiciones.findByVehiculoIdAndEmpresaIdOrderByTsDesc(
                v.getVehiculoId(), v.getEmpresaId(), PageRequest.of(0, 10));
        if (recientes.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El vehiculo aun no reporto posiciones");
        Posicion ultima = recientes.get(0);
        double distanciaKm = geo.distanciaRestanteM(v.getRutaId(), v.getEmpresaId(), ultima.getLat(), ultima.getLon())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada")) / 1000.0;
        double velMedia = Math.max(5.0, recientes.stream().mapToDouble(Posicion::getVelocidadKmh).average().orElse(5.0));
        ZonedDateTime ahora = ZonedDateTime.now(ZONA);
        Double ventanaMin = v.getFinPlan() == null ? null
                : Duration.between(Instant.now(), v.getFinPlan()).toSeconds() / 60.0;

        Map<String, Object> res = new LinkedHashMap<>(analytics.eta(new EtaRequest(
                distanciaKm, velMedia, ahora.getHour(), ahora.getDayOfWeek().getValue() - 1, ventanaMin)));
        res.put("distancia_restante_km", Math.round(distanciaKm * 100) / 100.0);
        res.put("velocidad_media_kmh", Math.round(velMedia * 10) / 10.0);
        return res;
    }

    /** Riesgo de anomalia de las ultimas posiciones frente a la ruta; con riesgo critico crea una alerta. */
    public Map<String, Object> riesgo(Long empresaId, Long vehiculoId, Long rutaId) {
        List<Punto> ruta = geo.puntosRuta(rutaId, empresaId);
        if (ruta.size() < 2) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada");
        List<Posicion> recientes = posiciones.findByVehiculoIdAndEmpresaIdOrderByTsDesc(vehiculoId, empresaId, PageRequest.of(0, 60));
        if (recientes.size() < 3) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Posiciones insuficientes");

        Map<String, Object> res = analytics.trayecto(new TrayectoRequest(
                ruta.stream().map(p -> new PuntoIn(p.lat(), p.lon())).toList(),
                recientes.stream().map(x -> new AnalyticsClient.PosicionIn(
                        x.getTs().toString(), x.getLat(), x.getLon(), x.getVelocidadKmh())).toList()));

        if (res.get("puntaje_riesgo") instanceof Number n && n.doubleValue() >= UMBRAL_CRITICO) {
            alertas.crearSiNoExiste(empresaId, vehiculoId, Alerta.ANOMALIA_TRAYECTO,
                    "Puntaje de riesgo " + n.intValue() + "/100 respecto de la ruta planificada");
        }
        return res;
    }
}
