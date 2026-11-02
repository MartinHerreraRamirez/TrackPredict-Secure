package com.trackpredict.security;

import com.trackpredict.repository.DispositivoRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/** Autentica a los dispositivos GPS por header X-Device-Key (se compara su hash SHA-256). */
public class DeviceKeyFilter extends OncePerRequestFilter {
    private final DispositivoRepository dispositivos;

    public DeviceKeyFilter(DispositivoRepository dispositivos) { this.dispositivos = dispositivos; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !(req.getMethod().equals("POST") && req.getRequestURI().equals("/api/posiciones"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String key = req.getHeader("X-Device-Key");
        if (key != null) {
            dispositivos.findByApiKeyHashAndActivoTrue(sha256(key)).ifPresent(d -> {
                var principal = new DevicePrincipal(d.getEmpresaId(), d.getVehiculoId());
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))));
            });
        }
        chain.doFilter(req, res);
    }

    public static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
