package com.trackpredict.web;

import com.trackpredict.domain.Conductor;
import com.trackpredict.repository.ConductorRepository;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AuditoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** CRUD de conductores. Contienen datos personales: lectura solo ADMIN y SUPERVISOR; escritura solo ADMIN. */
@RestController
@RequestMapping("/api/conductores")
public class ConductorController {
    public record ConductorDto(@NotBlank String nombre, @NotBlank String documento) {}

    private final ConductorRepository repo;
    private final AuditoriaService auditoria;

    public ConductorController(ConductorRepository repo, AuditoriaService auditoria) { this.repo = repo; this.auditoria = auditoria; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Conductor> listar(@AuthenticationPrincipal AuthPrincipal p) {
        return repo.findByEmpresaIdAndActivoTrueOrderByNombre(p.empresaId());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Conductor crear(@Valid @RequestBody ConductorDto dto, @AuthenticationPrincipal AuthPrincipal p) {
        String doc = dto.documento().trim();
        Conductor c = repo.findByEmpresaIdAndDocumento(p.empresaId(), doc).orElse(null);
        if (c != null && c.isActivo()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un conductor con ese documento");
        if (c == null) { c = new Conductor(); c.setEmpresaId(p.empresaId()); }
        c.setActivo(true);   // si estaba dado de baja, se reactiva
        c.setNombre(dto.nombre().trim()); c.setDocumento(doc);
        Conductor guardado = repo.save(c);
        auditoria.registrar(p.empresaId(), p.userId(), "CONDUCTOR_CREAR", "conductor:" + guardado.getId());
        return guardado;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Conductor actualizar(@PathVariable Long id, @Valid @RequestBody ConductorDto dto, @AuthenticationPrincipal AuthPrincipal p) {
        Conductor c = buscar(id, p);
        String doc = dto.documento().trim();
        if (!doc.equals(c.getDocumento()) && repo.existsByEmpresaIdAndDocumento(p.empresaId(), doc)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un conductor con ese documento");
        }
        c.setNombre(dto.nombre().trim()); c.setDocumento(doc);
        auditoria.registrar(p.empresaId(), p.userId(), "CONDUCTOR_ACTUALIZAR", "conductor:" + id);
        return repo.save(c);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        Conductor c = buscar(id, p);
        c.setActivo(false);   // baja logica
        repo.save(c);
        auditoria.registrar(p.empresaId(), p.userId(), "CONDUCTOR_BAJA", "conductor:" + id);
    }

    private Conductor buscar(Long id, AuthPrincipal p) {
        return repo.findByIdAndEmpresaIdAndActivoTrue(id, p.empresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conductor no encontrado"));
    }
}
