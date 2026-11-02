package com.trackpredict;

import com.trackpredict.service.RegistroService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistroServiceTest {
    @Test
    void aceptaContrasenaConLetrasYNumeros() {
        assertTrue(RegistroService.errorDePassword("Clave2026ok").isEmpty());
    }

    @Test
    void rechazaContrasenasDebiles() {
        assertTrue(RegistroService.errorDePassword("corta1").isPresent());          // menos de 8
        assertTrue(RegistroService.errorDePassword("sololetrasaqui").isPresent());  // sin numeros
        assertTrue(RegistroService.errorDePassword("123456789").isPresent());       // sin letras
        assertTrue(RegistroService.errorDePassword(null).isPresent());
        assertTrue(RegistroService.errorDePassword("a1".repeat(40)).isPresent());   // mas de 72 bytes (limite de BCrypt)
    }

    @Test
    void normalizaEmailYEspacios() {
        assertEquals("ana@ejemplo.com", RegistroService.normalizarEmail("  Ana@Ejemplo.COM "));
        assertEquals("Seguridad del Sur", RegistroService.normalizarEspacios("  Seguridad   del  Sur "));
    }
}
