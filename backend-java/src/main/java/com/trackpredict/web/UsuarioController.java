package com.trackpredict.web;

import com.trackpredict.domain.Usuario;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestion de usuarios de la empresa. Solo ADMIN. Nunca devuelve el hash de la contrasena. */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    public record UsuarioVista(Long id, String email, String nombre, String rol, boolean activo) {
        static UsuarioVista de(Usuario u) { return new UsuarioVista(u.getId(), u.getEmail(), u.getNombre(), u.getRol(), u.isActivo()); }
    }
    public record NuevoUsuario(@NotBlank @Size(max = 120) String nombre, @NotBlank @Email @Size(max = 160) String email,
                               @NotBlank @Size(max = 200) String password, @NotBlank String rol) {}
    public record CambioUsuario(@NotBlank @Size(max = 120) String nombre, @NotBlank String rol, boolean activo,
                                @Size(max = 200) String passwordNueva) {}

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) { this.servicio = servicio; }

    @GetMapping
    public List<UsuarioVista> listar(@AuthenticationPrincipal AuthPrincipal p) {
        return servicio.listar(p.empresaId()).stream().map(UsuarioVista::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioVista crear(@Valid @RequestBody NuevoUsuario req, @AuthenticationPrincipal AuthPrincipal p) {
        return UsuarioVista.de(servicio.crear(p.empresaId(), p.userId(), req.nombre(), req.email(), req.password(), req.rol()));
    }

    @PutMapping("/{id}")
    public UsuarioVista actualizar(@PathVariable Long id, @Valid @RequestBody CambioUsuario req, @AuthenticationPrincipal AuthPrincipal p) {
        return UsuarioVista.de(servicio.actualizar(p.empresaId(), p.userId(), id, req.nombre(), req.rol(), req.activo(), req.passwordNueva()));
    }
}
