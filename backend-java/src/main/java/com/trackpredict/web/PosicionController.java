package com.trackpredict.web;

import com.trackpredict.domain.Posicion;
import com.trackpredict.domain.Viaje;
import com.trackpredict.domain.ViajeEstado;
import com.trackpredict.repository.PosicionRepository;
import com.trackpredict.repository.ViajeRepository;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.security.DevicePrincipal;
import com.trackpredict.service.AlertaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
public class PosicionController {
    public record PosicionIn(@NotNull Instant ts,
                             @DecimalMin("-90") @DecimalMax("90") double lat,
                             @DecimalMin("-180") @DecimalMax("180") double lon,
                             @PositiveOrZero double velocidadKmh) {}

    private final PosicionRepository repo;
    private final ViajeRepository viajes;
    private final AlertaService alertas;

    public PosicionController(PosicionRepository repo, ViajeRepository viajes, AlertaService alertas) {
        this.repo = repo; this.viajes = viajes; this.alertas = alertas;
    }

    /** Ingesta: la llama el dispositivo GPS (X-Device-Key). Empresa y vehiculo salen de la credencial, no del body. */
    @PostMapping("/api/posiciones")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingestar(@Valid @RequestBody PosicionIn in, @AuthenticationPrincipal DevicePrincipal d) {
        Optional<Viaje> viaje = viajes.findFirstByVehiculoIdAndEmpresaIdAndEstado(d.vehiculoId(), d.empresaId(), ViajeEstado.EN_CURSO);
        Posicion nueva = new Posicion(d.empresaId(), d.vehiculoId(), in.ts(), in.lat(), in.lon(), in.velocidadKmh());
        viaje.ifPresent(v -> nueva.setViajeId(v.getId()));
        Posicion p = repo.save(nueva);
        alertas.evaluar(p, viaje.map(Viaje::getRutaId).orElse(null));
    }

    /** Ultima posicion conocida de cada vehiculo de la empresa (alimenta el mapa en vivo). */
    @GetMapping("/api/posiciones/ultimas")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR')")
    public List<Posicion> ultimas(@AuthenticationPrincipal AuthPrincipal p) {
        return repo.ultimasPorVehiculo(p.empresaId());
    }
}
