package com.trackpredict.web;

import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public List<Map<String, Object>> ultimas(@AuthenticationPrincipal AuthPrincipal p) {
        return service.ultimas(p.empresaId());
    }
}
