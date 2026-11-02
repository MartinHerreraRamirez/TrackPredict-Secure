package com.trackpredict.web;

import com.trackpredict.domain.Alerta;
import com.trackpredict.repository.AlertaRepository;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AuditoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {
    public record EstadoDto(@Pattern(regexp = "RECONOCIDA|EN_ATENCION|CERRADA") String estado) {}

    private final AlertaRepository repo;
    private final AuditoriaService auditoria;

    public AlertaController(AlertaRepository repo, AuditoriaService auditoria) {
        this.repo = repo; this.auditoria = auditoria;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR','AUDITOR')")
    public List<Alerta> listar(@AuthenticationPrincipal AuthPrincipal p) {
        return repo.findTop100ByEmpresaIdOrderByCreadaEnDesc(p.empresaId());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','SUPERVISOR')")
    public Alerta cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoDto dto, @AuthenticationPrincipal AuthPrincipal p) {
        Alerta a = repo.findByIdAndEmpresaId(id, p.empresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta no encontrada"));
        a.setEstado(dto.estado());
        auditoria.registrar(p.empresaId(), p.userId(), "ALERTA_" + dto.estado(), "alerta:" + id);
        return repo.save(a);
    }
}
