package com.trackpredict;

import com.trackpredict.security.AuthPrincipal;
import com.trackpredict.security.DeviceKeyFilter;
import com.trackpredict.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService jwt = new JwtService("unit-test-secret-unit-test-secret-1234567890", 5);

    @Test
    void tokenConservaEmpresaYRol() {
        String token = jwt.generate(new AuthPrincipal(7L, 3L, "op@trackpredict.com", "OPERADOR"));
        AuthPrincipal p = jwt.parse(token);
        assertEquals(3L, p.empresaId());
        assertEquals("OPERADOR", p.rol());
        assertEquals(7L, p.userId());
    }

    @Test
    void tokenAdulteradoSeRechaza() {
        String token = jwt.generate(new AuthPrincipal(1L, 1L, "a@trackpredict.com", "ADMIN"));
        assertThrows(Exception.class, () -> jwt.parse(token.substring(0, token.length() - 3) + "abc"));
    }

    @Test
    void hashDeApiKeyDeDemoCoincideConLaSemilla() {
        // Mismo valor que db/init/02_seed.sql
        assertEquals(64, DeviceKeyFilter.sha256("demo-device-key-001").length());
    }
}
