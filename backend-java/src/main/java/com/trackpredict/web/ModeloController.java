package com.trackpredict.web;

import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AnalyticsClient;
import com.trackpredict.service.AuditoriaService;
import com.trackpredict.service.ReentrenamientoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Calidad y reentrenamiento de los modelos predictivos. Los modelos son globales (no contienen datos de ninguna
 * empresa), por eso cualquier usuario autenticado ve sus metricas; reentrenar es solo del ADMIN.
 * El front nunca llama a Python directamente.
 */
@RestController
@RequestMapping("/api/modelos")
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR','AUDITOR')")
public class ModeloController {
    private final AnalyticsClient analytics;
    private final ReentrenamientoService reentrenamiento;
    private final AuditoriaService auditoria;

    public ModeloController(AnalyticsClient analytics, ReentrenamientoService reentrenamiento, AuditoriaService auditoria) {
        this.analytics = analytics; this.reentrenamiento = reentrenamiento; this.auditoria = auditoria;
    }

    @GetMapping("/metricas")
    public Map<String, Object> metricas() { return analytics.modelos(); }

    /** Cuantos viajes finalizados de la empresa sirven para reentrenar y cuantos hacen falta. */
    @GetMapping("/reentrenamiento")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> estadoReentrenamiento(@AuthenticationPrincipal AuthPrincipal p) {
        return Map.of("viajesDisponibles", reentrenamiento.observaciones(p.empresaId()).size(),
                "minimoRequerido", ReentrenamientoService.MINIMO);
    }

    @PostMapping("/reentrenar")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> reentrenar(@AuthenticationPrincipal AuthPrincipal p) {
        Map<String, Object> res = reentrenamiento.reentrenar(p.empresaId());
        auditoria.registrar(p.empresaId(), p.userId(), "MODELO_REENTRENAR", "eta");
        return res;
    }
}
