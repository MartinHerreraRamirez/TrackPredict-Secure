package com.trackpredict;

import com.trackpredict.domain.Usuario;
import com.trackpredict.repository.UsuarioRepository;
import com.trackpredict.service.AuditoriaService;
import com.trackpredict.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    UsuarioRepository repo = mock(UsuarioRepository.class);
    AuditoriaService auditoria = mock(AuditoriaService.class);
    UsuarioService servicio;
    Usuario admin, otroAdmin, operador;

    /** Usuario con id asignado por reflexion (en produccion lo asigna la base). */
    static Usuario con(long id, String email, String rol, boolean activo) throws Exception {
        Usuario u = new Usuario(1L, email, "hash", "Nombre", rol);
        var f = Usuario.class.getDeclaredField("id"); f.setAccessible(true); f.set(u, id);
        u.setActivo(activo);
        return u;
    }

    @BeforeEach
    void preparar() throws Exception {
        servicio = new UsuarioService(repo, new BCryptPasswordEncoder(4), auditoria);
        admin = con(1, "a@x.com", "ADMIN", true);
        otroAdmin = con(2, "b@x.com", "ADMIN", true);
        operador = con(3, "o@x.com", "OPERADOR", true);
        when(repo.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(repo.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(admin));
        when(repo.findByIdAndEmpresaId(2L, 1L)).thenReturn(Optional.of(otroAdmin));
        when(repo.findByIdAndEmpresaId(3L, 1L)).thenReturn(Optional.of(operador));
    }

    @Test
    void creaUsuarioConRolYContrasenaHasheada() {
        Usuario u = servicio.crear(1L, 1L, "  Ana   Perez ", "ANA@X.com", "Clave1234", "SUPERVISOR");
        assertEquals("ana@x.com", u.getEmail());
        assertEquals("Ana Perez", u.getNombre());
        assertEquals("SUPERVISOR", u.getRol());
        assertNotEquals("Clave1234", u.getPasswordHash());
    }

    @Test
    void rechazaEmailRepetidoRolInvalidoYContrasenaDebil() {
        when(repo.existsByEmail("o@x.com")).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> servicio.crear(1L, 1L, "N", "o@x.com", "Clave1234", "AUDITOR")).getStatusCode().value());
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(1L, 1L, "N", "n@x.com", "Clave1234", "SUPERUSUARIO"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(1L, 1L, "N", "n@x.com", "corta", "AUDITOR"));
    }

    @Test
    void noPermiteCambiarElPropioRolNiDarseDeBaja() {
        assertThrows(ResponseStatusException.class, () -> servicio.actualizar(1L, 1L, 1L, "N", "OPERADOR", true, null));
        assertThrows(ResponseStatusException.class, () -> servicio.actualizar(1L, 1L, 1L, "N", "ADMIN", false, null));
    }

    @Test
    void laEmpresaConservaAlMenosUnAdminActivo() {
        // otroAdmin esta inactivo: admin es el unico activo, asi que otro actor no puede degradarlo
        otroAdmin.setActivo(false);
        when(repo.findByEmpresaIdOrderByNombre(1L)).thenReturn(List.of(admin, otroAdmin, operador));
        assertThrows(ResponseStatusException.class, () -> servicio.actualizar(1L, 3L, 1L, "N", "OPERADOR", true, null));
    }

    @Test
    void permiteDegradarAdminSiHayOtroActivoYCambiarContrasena() {
        when(repo.findByEmpresaIdOrderByNombre(1L)).thenReturn(List.of(admin, otroAdmin, operador));
        Usuario u = servicio.actualizar(1L, 1L, 2L, "Nuevo Nombre", "AUDITOR", true, "Otra12345");
        assertEquals("AUDITOR", u.getRol());
        assertEquals("Nuevo Nombre", u.getNombre());
        assertTrue(new BCryptPasswordEncoder(4).matches("Otra12345", u.getPasswordHash()));
    }

    @Test
    void usuarioDeOtraEmpresaNoSeEncuentra() {
        when(repo.findByIdAndEmpresaId(9L, 1L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> servicio.actualizar(1L, 1L, 9L, "N", "ADMIN", true, null)).getStatusCode().value());
    }
}
