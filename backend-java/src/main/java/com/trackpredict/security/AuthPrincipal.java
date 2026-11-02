package com.trackpredict.security;

/** Usuario humano autenticado via JWT. empresaId aisla los datos entre empresas (multi-tenant). */
public record AuthPrincipal(Long userId, Long empresaId, String email, String rol) {}
