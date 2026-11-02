package com.trackpredict.web;

import com.trackpredict.domain.Usuario;
import com.trackpredict.repository.UsuarioRepository;
import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.security.JwtService;
import com.trackpredict.service.AuditoriaService;
import com.trackpredict.service.RegistroService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record RegistroRequest(@NotBlank @Size(max = 120) String empresa, @NotBlank @Size(max = 120) String nombre,
                                  @NotBlank @Email @Size(max = 160) String email, @NotBlank @Size(max = 200) String password) {}
    public record LoginResponse(String token, String rol, String nombre, Long empresaId, Long usuarioId) {}

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditoriaService auditoria;
    private final RegistroService registro;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt, AuditoriaService auditoria,
                          RegistroService registro) {
        this.usuarios = usuarios; this.encoder = encoder; this.jwt = jwt; this.auditoria = auditoria; this.registro = registro;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario u = usuarios.findByEmailAndActivoTrue(req.email().trim().toLowerCase())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
        var principal = new AuthPrincipal(u.getId(), u.getEmpresaId(), u.getEmail(), u.getRol());
        auditoria.registrar(u.getEmpresaId(), u.getId(), "LOGIN", u.getEmail());
        return new LoginResponse(jwt.generate(principal), u.getRol(), u.getNombre(), u.getEmpresaId(), u.getId());
    }

    /** Registro publico: crea una empresa y a su primer usuario (ADMIN) y devuelve la sesion ya iniciada. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registro(@Valid @RequestBody RegistroRequest req) {
        Usuario u = registro.registrar(req.empresa(), req.nombre(), req.email(), req.password());
        var principal = new AuthPrincipal(u.getId(), u.getEmpresaId(), u.getEmail(), u.getRol());
        return new LoginResponse(jwt.generate(principal), u.getRol(), u.getNombre(), u.getEmpresaId(), u.getId());
    }

    @GetMapping("/me")
    public AuthPrincipal me(@AuthenticationPrincipal AuthPrincipal p) { return p; }
}
