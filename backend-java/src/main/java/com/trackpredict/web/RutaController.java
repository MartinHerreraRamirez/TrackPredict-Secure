package com.trackpredict.web;

import com.trackpredict.repository.GeoRepository;
import com.trackpredict.repository.GeoRepository.RutaInfo;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AuditoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Rutas planificadas. Lectura: ADMIN, OPERADOR, SUPERVISOR. Alta: ADMIN y SUPERVISOR. */
@RestController
@RequestMapping("/api/rutas")
public class RutaController {
    public record RutaDto(@NotBlank @Size(max = 120) String nombre,
                          @Min(1) @Max(1440) int duracionPlanMin,
                          @Min(10) @Max(200) int velocidadMaxKmh,
                          @NotNull @Size(min = 2, max = 500) List<@Valid PuntoDto> puntos) {}

    private final GeoRepository geo;
    private final AuditoriaService auditoria;

    public RutaController(GeoRepository geo, AuditoriaService auditoria) { this.geo = geo; this.auditoria = auditoria; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR')")
    public List<RutaInfo> listar(@AuthenticationPrincipal AuthPrincipal p) { return geo.rutas(p.empresaId()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR')")
    public RutaInfo obtener(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        return geo.ruta(id, p.empresaId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public RutaInfo crear(@Valid @RequestBody RutaDto dto, @AuthenticationPrincipal AuthPrincipal p) {
        Long id = geo.crearRuta(p.empresaId(), dto.nombre().trim(), PuntoDto.aPuntos(dto.puntos()),
                dto.duracionPlanMin(), dto.velocidadMaxKmh());
        auditoria.registrar(p.empresaId(), p.userId(), "RUTA_CREAR", dto.nombre());
        return geo.ruta(id, p.empresaId()).orElseThrow();
    }
}
