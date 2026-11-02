package com.trackpredict.service;

import com.trackpredict.domain.Usuario;
import com.trackpredict.repository.UsuarioRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Registro de usuarios: crea una empresa nueva y su primer usuario, que queda como ADMIN de esa empresa.
 * Los demas roles (operador, supervisor, auditor) los asigna luego un administrador. Cada empresa queda aislada de las otras.
 */
@Service
public class RegistroService {
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX_BYTES = 72;   // limite de BCrypt: mas alla de 72 bytes ignora el resto

    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public RegistroService(UsuarioRepository usuarios, JdbcTemplate jdbc, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.usuarios = usuarios; this.jdbc = jdbc; this.encoder = encoder; this.auditoria = auditoria;
    }

    /** Reglas de contrasena: vacio si es valida, o el motivo del rechazo. */
    public static Optional<String> errorDePassword(String p) {
        if (p == null || p.length() < PASSWORD_MIN) return Optional.of("La contrasena debe tener al menos " + PASSWORD_MIN + " caracteres");
        if (p.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) return Optional.of("La contrasena es demasiado larga");
        boolean letra = p.chars().anyMatch(Character::isLetter);
        boolean digito = p.chars().anyMatch(Character::isDigit);
        if (!letra || !digito) return Optional.of("La contrasena debe combinar letras y numeros");
        return Optional.empty();
    }

    public static String normalizarEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    public static String normalizarEspacios(String texto) { return texto.trim().replaceAll("\\s+", " "); }

    @Transactional
    public Usuario registrar(String empresa, String nombre, String email, String password) {
        errorDePassword(password).ifPresent(m -> { throw new IllegalArgumentException(m); });
        String mail = normalizarEmail(email);
        if (usuarios.existsByEmail(mail)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email");

        Long empresaId;
        try {
            empresaId = jdbc.queryForObject("INSERT INTO empresa (nombre) VALUES (?) RETURNING id", Long.class, normalizarEspacios(empresa));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una empresa con ese nombre");
        }
        Usuario u = usuarios.save(new Usuario(empresaId, mail, encoder.encode(password), normalizarEspacios(nombre), "ADMIN"));
        auditoria.registrar(empresaId, u.getId(), "REGISTRO", mail);
        return u;
    }
}
