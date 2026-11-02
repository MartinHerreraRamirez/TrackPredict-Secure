package com.trackpredict.web;

import com.trackpredict.domain.Viaje;
import com.trackpredict.domain.ViajeEstado;
import com.trackpredict.repository.ConductorRepository;
import com.trackpredict.repository.GeoRepository;
import com.trackpredict.repository.VehiculoRepository;
import com.trackpredict.repository.ViajeRepository;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AuditoriaService;
import com.trackpredict.service.ViajeAnalisisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Viajes: planificacion, ciclo de vida (iniciar/finalizar/cancelar) y analisis predictivo en vivo. */
@RestController
@RequestMapping("/api/viajes")
public class ViajeController {
    public record ViajeDto(@NotNull Long vehiculoId, Long conductorId, @NotNull Long rutaId, Instant inicioPlan, Instant finPlan) {}

    private final ViajeRepository viajes;
    private final VehiculoRepository vehiculos;
    private final ConductorRepository conductores;
    private final GeoRepository geo;
    private final ViajeAnalisisService analisis;
    private final AuditoriaService auditoria;

    public ViajeController(ViajeRepository viajes, VehiculoRepository vehiculos, ConductorRepository conductores,
                           GeoRepository geo, ViajeAnalisisService analisis, AuditoriaService auditoria) {
        this.viajes = viajes; this.vehiculos = vehiculos; this.conductores = conductores;
        this.geo = geo; this.analisis = analisis; this.auditoria = auditoria;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR','AUDITOR')")
    public List<Viaje> listar(@AuthenticationPrincipal AuthPrincipal p) {
        return viajes.findTop50ByEmpresaIdOrderByIdDesc(p.empresaId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Viaje planificar(@Valid @RequestBody ViajeDto dto, @AuthenticationPrincipal AuthPrincipal p) {
        vehiculos.findByIdAndEmpresaIdAndActivoTrue(dto.vehiculoId(), p.empresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehiculo no encontrado"));
        if (!geo.existeRuta(dto.rutaId(), p.empresaId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada");
        if (dto.conductorId() != null) {
            conductores.findByIdAndEmpresaIdAndActivoTrue(dto.conductorId(), p.empresaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conductor no encontrado"));
        }
        Viaje v = viajes.save(new Viaje(p.empresaId(), dto.vehiculoId(), dto.conductorId(), dto.rutaId(), dto.inicioPlan(), dto.finPlan()));
        auditoria.registrar(p.empresaId(), p.userId(), "VIAJE_PLANIFICAR", "viaje:" + v.getId());
        return v;
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Viaje iniciar(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        Viaje previo = buscar(id, p);
        viajes.findFirstByVehiculoIdAndEmpresaIdAndEstado(previo.getVehiculoId(), p.empresaId(), ViajeEstado.EN_CURSO)
                .filter(otro -> !otro.getId().equals(id)).ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El vehiculo ya tiene un viaje en curso");
                });
        Viaje v = cambiarEstado(id, ViajeEstado.EN_CURSO, p);
        Instant ahora = Instant.now();
        v.setInicioReal(ahora);
        if (v.getFinPlan() == null || v.getFinPlan().isBefore(ahora)) {   // ventana = duracion planificada de la ruta
            int min = geo.duracionPlanMin(v.getRutaId(), p.empresaId()).orElse(30);
            v.setFinPlan(ahora.plus(Duration.ofMinutes(min)));
        }
        return viajes.save(v);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Viaje finalizar(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        Viaje v = cambiarEstado(id, ViajeEstado.FINALIZADO, p);
        v.setFinReal(Instant.now());
        return viajes.save(v);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Viaje cancelar(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        return viajes.save(cambiarEstado(id, ViajeEstado.CANCELADO, p));
    }

    /** ETA en vivo del viaje (modelo de Python). */
    @GetMapping("/{id}/eta")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR')")
    public Map<String, Object> eta(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        return analisis.eta(enCurso(id, p));
    }

    /** Puntaje de riesgo de anomalia del viaje en curso (modelo de Python). */
    @GetMapping("/{id}/riesgo")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR')")
    public Map<String, Object> riesgo(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        Viaje v = enCurso(id, p);
        return analisis.riesgo(p.empresaId(), v.getVehiculoId(), v.getRutaId());
    }

    private Viaje enCurso(Long id, AuthPrincipal p) {
        Viaje v = buscar(id, p);
        if (!ViajeEstado.EN_CURSO.equals(v.getEstado())) throw new ResponseStatusException(HttpStatus.CONFLICT, "El viaje no esta en curso");
        return v;
    }

    private Viaje cambiarEstado(Long id, String destino, AuthPrincipal p) {
        Viaje v = buscar(id, p);
        if (!ViajeEstado.transicionValida(v.getEstado(), destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transicion invalida: " + v.getEstado() + " -> " + destino);
        }
        v.setEstado(destino);
        auditoria.registrar(p.empresaId(), p.userId(), "VIAJE_" + destino, "viaje:" + id);
        return v;
    }

    private Viaje buscar(Long id, AuthPrincipal p) {
        return viajes.findByIdAndEmpresaId(id, p.empresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
    }
}
