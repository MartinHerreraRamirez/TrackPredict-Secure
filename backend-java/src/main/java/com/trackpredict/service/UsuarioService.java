package com.trackpredict.service;

import com.trackpredict.domain.Usuario;
import com.trackpredict.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Administracion de usuarios de una empresa (solo ADMIN). Siempre acotada a la empresa de quien opera.
 * Reglas: no se puede quitar el rol ni dar de baja a uno mismo, y la empresa nunca queda sin un ADMIN activo.
 */
@Service
public class UsuarioService {
    public static final Set<String> ROLES = Set.of("ADMIN", "OPERADOR", "SUPERVISOR", "AUDITOR");

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.repo = repo; this.encoder = encoder; this.auditoria = auditoria;
    }

    public List<Usuario> listar(Long empresaId) { return repo.findByEmpresaIdOrderByNombre(empresaId); }

    public Usuario crear(Long empresaId, Long actorId, String nombre, String email, String password, String rol) {
        validarRol(rol);
        RegistroService.errorDePassword(password).ifPresent(m -> { throw new IllegalArgumentException(m); });
        String mail = RegistroService.normalizarEmail(email);
        if (repo.existsByEmail(mail)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email");
        Usuario u = repo.save(new Usuario(empresaId, mail, encoder.encode(password), RegistroService.normalizarEspacios(nombre), rol));
        auditoria.registrar(empresaId, actorId, "USUARIO_CREAR", mail + " (" + rol + ")");
        return u;
    }

    public Usuario actualizar(Long empresaId, Long actorId, Long id, String nombre, String rol, boolean activo, String passwordNueva) {
        validarRol(rol);
        Usuario u = repo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        boolean esUnoMismo = u.getId().equals(actorId);
        if (esUnoMismo && (!rol.equals(u.getRol()) || !activo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No podes cambiar tu propio rol ni darte de baja");
        }
        boolean dejaDeSerAdminActivo = u.isActivo() && "ADMIN".equals(u.getRol()) && (!activo || !"ADMIN".equals(rol));
        if (dejaDeSerAdminActivo && adminsActivos(empresaId) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La empresa debe conservar al menos un administrador activo");
        }
        u.setNombre(RegistroService.normalizarEspacios(nombre));
        u.setRol(rol);
        u.setActivo(activo);
        if (passwordNueva != null && !passwordNueva.isBlank()) {
            RegistroService.errorDePassword(passwordNueva).ifPresent(m -> { throw new IllegalArgumentException(m); });
            u.setPasswordHash(encoder.encode(passwordNueva));
        }
        auditoria.registrar(empresaId, actorId, "USUARIO_ACTUALIZAR", u.getEmail() + " (" + rol + (activo ? "" : ", baja") + ")");
        return repo.save(u);
    }

    private long adminsActivos(Long empresaId) {
        return repo.findByEmpresaIdOrderByNombre(empresaId).stream().filter(x -> x.isActivo() && "ADMIN".equals(x.getRol())).count();
    }

    private static void validarRol(String rol) {
        if (rol == null || !ROLES.contains(rol)) throw new IllegalArgumentException("Rol invalido");
    }
}
