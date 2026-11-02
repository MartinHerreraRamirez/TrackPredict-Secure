package com.trackpredict.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Bitacora de auditoria: la aplicacion solo inserta y consulta (un trigger en BD impide UPDATE/DELETE). */
@Service
public class AuditoriaService {
    private final JdbcTemplate jdbc;

    public AuditoriaService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void registrar(Long empresaId, Long usuarioId, String accion, String recurso) {
        jdbc.update("INSERT INTO auditoria (empresa_id, usuario_id, accion, recurso) VALUES (?,?,?,?)",
                empresaId, usuarioId, accion, recurso);
    }

    public List<Map<String, Object>> ultimas(Long empresaId) {
        return jdbc.queryForList(
                "SELECT id, usuario_id, accion, recurso, ts FROM auditoria WHERE empresa_id = ? ORDER BY id DESC LIMIT 100",
                empresaId);
    }
}
