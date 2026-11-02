package com.trackpredict.web;

import com.trackpredict.domain.Vehiculo;
import com.trackpredict.repository.VehiculoRepository;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AuditoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** CRUD de vehiculos. Lectura: todos los roles. Escritura: solo ADMIN. Siempre acotado a la empresa del usuario. */
@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {
    public record VehiculoDto(@NotBlank String patente,
                              @Pattern(regexp = "BLINDADO|AUTO|ESCOLTA") String tipo,
                              String marca, String modelo) {}

    private final VehiculoRepository repo;
    private final AuditoriaService auditoria;

    public VehiculoController(VehiculoRepository repo, AuditoriaService auditoria) {
        this.repo = repo; this.auditoria = auditoria;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR','AUDITOR')")
    public List<Vehiculo> listar(@AuthenticationPrincipal AuthPrincipal p) {
        return repo.findByEmpresaIdAndActivoTrueOrderByPatente(p.empresaId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR','AUDITOR')")
    public Vehiculo obtener(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        return buscar(id, p);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Vehiculo crear(@Valid @RequestBody VehiculoDto dto, @AuthenticationPrincipal AuthPrincipal p) {
        String patente = dto.patente().trim().toUpperCase();
        Vehiculo v = repo.findByEmpresaIdAndPatente(p.empresaId(), patente).orElse(null);
        if (v != null && v.isActivo()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un vehiculo con esa patente");
        if (v == null) { v = new Vehiculo(); v.setEmpresaId(p.empresaId()); }
        v.setActivo(true);   // si estaba dada de baja, se reactiva con sus datos nuevos
        aplicar(v, dto, patente);
        Vehiculo guardado = repo.save(v);
        auditoria.registrar(p.empresaId(), p.userId(), "VEHICULO_CREAR", patente);
        return guardado;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Vehiculo actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoDto dto, @AuthenticationPrincipal AuthPrincipal p) {
        Vehiculo v = buscar(id, p);
        String patente = dto.patente().trim().toUpperCase();
        if (!patente.equals(v.getPatente()) && repo.existsByEmpresaIdAndPatente(p.empresaId(), patente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un vehiculo con esa patente");
        }
        aplicar(v, dto, patente);
        auditoria.registrar(p.empresaId(), p.userId(), "VEHICULO_ACTUALIZAR", v.getPatente());
        return repo.save(v);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        Vehiculo v = buscar(id, p);
        v.setActivo(false);   // baja logica: se conserva el historial de viajes y posiciones
        repo.save(v);
        auditoria.registrar(p.empresaId(), p.userId(), "VEHICULO_BAJA", v.getPatente());
    }

    private Vehiculo buscar(Long id, AuthPrincipal p) {
        return repo.findByIdAndEmpresaIdAndActivoTrue(id, p.empresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehiculo no encontrado"));
    }

    private static void aplicar(Vehiculo v, VehiculoDto dto, String patente) {
        v.setPatente(patente); v.setTipo(dto.tipo());
        v.setMarca(dto.marca() == null || dto.marca().isBlank() ? null : dto.marca().trim());
        v.setModelo(dto.modelo() == null || dto.modelo().isBlank() ? null : dto.modelo().trim());
    }
}
