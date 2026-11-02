package com.trackpredict.web;

import com.trackpredict.repository.VehiculoRepository;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AnalyticsClient;
import com.trackpredict.service.AnalyticsClient.EtaRequest;
import com.trackpredict.service.ViajeAnalisisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Pasarela hacia el microservicio Python: el front SOLO habla con Java, que aplica RBAC y aislamiento
 * por empresa antes de delegar el calculo analitico.
 */
@RestController
@RequestMapping("/api/predicciones")
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR')")
public class PrediccionController {
    public record EtaDto(@Min(0) double distanciaRestanteKm, @Min(1) double velocidadMediaKmh,
                         int hora, int diaSemana, Double minutosVentanaRestante) {}
    public record TrayectoDto(@NotNull Long vehiculoId, @NotNull Long rutaId) {}

    private final AnalyticsClient analytics;
    private final VehiculoRepository vehiculos;
    private final ViajeAnalisisService analisis;

    public PrediccionController(AnalyticsClient analytics, VehiculoRepository vehiculos, ViajeAnalisisService analisis) {
        this.analytics = analytics; this.vehiculos = vehiculos; this.analisis = analisis;
    }

    /** Escenario "que pasaria si": ETA para una distancia, velocidad y hora elegidas por el usuario. */
    @PostMapping("/eta")
    public Map<String, Object> eta(@Valid @RequestBody EtaDto d) {
        return analytics.eta(new EtaRequest(d.distanciaRestanteKm(), d.velocidadMediaKmh(), d.hora(), d.diaSemana(),
                d.minutosVentanaRestante()));
    }

    @PostMapping("/trayecto")
    public Map<String, Object> trayecto(@Valid @RequestBody TrayectoDto d, @AuthenticationPrincipal AuthPrincipal p) {
        vehiculos.findByIdAndEmpresaIdAndActivoTrue(d.vehiculoId(), p.empresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehiculo no encontrado"));
        return analisis.riesgo(p.empresaId(), d.vehiculoId(), d.rutaId());
    }
}
