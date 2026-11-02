package com.trackpredict.web;

import com.trackpredict.repository.GeoRepository;
import com.trackpredict.repository.GeoRepository.GeocercaInfo;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AuditoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Geocercas: corredor autorizado de una ruta. Si un vehiculo en viaje sale de todas las de su ruta, se genera una alerta. */
@RestController
@RequestMapping("/api/geocercas")
public class GeocercaController {
    public record GeocercaDto(@NotNull Long rutaId, @NotBlank @Size(max = 120) String nombre,
                              @NotNull @Size(min = 3, max = 500) List<@Valid PuntoDto> puntos) {}

    private final GeoRepository geo;
    private final AuditoriaService auditoria;

    public GeocercaController(GeoRepository geo, AuditoriaService auditoria) { this.geo = geo; this.auditoria = auditoria; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR')")
    public List<GeocercaInfo> listar(@AuthenticationPrincipal AuthPrincipal p) { return geo.geocercas(p.empresaId()); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public GeocercaInfo crear(@Valid @RequestBody GeocercaDto dto, @AuthenticationPrincipal AuthPrincipal p) {
        if (!geo.existeRuta(dto.rutaId(), p.empresaId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada");
        Long id = geo.crearGeocerca(p.empresaId(), dto.rutaId(), dto.nombre().trim(), PuntoDto.aPuntos(dto.puntos()));
        auditoria.registrar(p.empresaId(), p.userId(), "GEOCERCA_CREAR", dto.nombre());
        return geo.geocercas(p.empresaId()).stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        if (!geo.eliminarGeocerca(id, p.empresaId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Geocerca no encontrada");
        auditoria.registrar(p.empresaId(), p.userId(), "GEOCERCA_ELIMINAR", "geocerca:" + id);
    }
}
