package com.trackpredict.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Cliente del microservicio Python (red interna). Autenticado con token interno. */
@Service
public class AnalyticsClient {
    public record EtaRequest(double distancia_restante_km, double velocidad_media_kmh, int hora, int dia_semana,
                             Double minutos_ventana_restante) {}
    public record PuntoIn(double lat, double lon) {}
    public record PosicionIn(String ts, double lat, double lon, double velocidad_kmh) {}
    public record TrayectoRequest(List<PuntoIn> ruta, List<PosicionIn> posiciones) {}

    public record ObservacionIn(double distancia_km, double velocidad_media_kmh, int hora, int dia_semana, double minutos_reales) {}
    public record ReentrenarRequest(List<ObservacionIn> observaciones) {}

    private static final Logger log = LoggerFactory.getLogger(AnalyticsClient.class);
    private final RestClient http;

    public AnalyticsClient(@Value("${trackpredict.analytics.base-url}") String baseUrl,
                           @Value("${trackpredict.analytics.internal-token}") String token) {
        // HTTP/1.1 explicito: el cliente del JDK intenta subir a HTTP/2 (h2c) y uvicorn (Python) corta esa conexion.
        HttpClient cliente = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(20));
        this.http = RestClient.builder().requestFactory(fabrica).baseUrl(baseUrl).defaultHeader("X-Internal-Token", token).build();
    }

    public Map<String, Object> eta(EtaRequest req) { return post("/predict/eta", req); }

    public Map<String, Object> trayecto(TrayectoRequest req) { return post("/analyze/trayecto", req); }

    public Map<String, Object> reentrenar(ReentrenarRequest req) { return post("/models/retrain", req); }

    /** Metricas de calidad de los modelos vigentes (MAE, R2, deteccion de anomalias...). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> modelos() {
        try {
            return http.get().uri("/models/info").retrieve().body(Map.class);
        } catch (RestClientException e) {
            log.warn("Fallo GET /models/info: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Servicio analitico no disponible");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Object body) {
        try {
            return http.post().uri(path).body(body).retrieve().body(Map.class);
        } catch (RestClientException e) {
            log.warn("Fallo POST {}: {}", path, e.getMessage());   // la causa real queda en los logs del backend
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Servicio analitico no disponible");
        }
    }
}
