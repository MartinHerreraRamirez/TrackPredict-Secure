package com.trackpredict.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Acceso a rutas y geocercas con consultas geoespaciales PostGIS. Siempre acotado por empresa_id. */
@Repository
public class GeoRepository {
    public record Punto(double lat, double lon) {}
    public record RutaInfo(Long id, String nombre, int duracionPlanMin, int velocidadMaxKmh, List<Punto> puntos) {}
    public record GeocercaInfo(Long id, Long rutaId, String nombre, List<Punto> puntos) {}

    private static final String DUMP_RUTA =
            "SELECT ST_Y((dp).geom) AS lat, ST_X((dp).geom) AS lon FROM " +
            "(SELECT ST_DumpPoints(trazado::geometry) AS dp FROM ruta WHERE id = ? AND empresa_id = ?) t " +
            "ORDER BY (dp).path[1]";
    private static final String DUMP_GEOCERCA =
            "SELECT ST_Y((dp).geom) AS lat, ST_X((dp).geom) AS lon FROM " +
            "(SELECT ST_DumpPoints(area::geometry) AS dp FROM geocerca WHERE id = ? AND empresa_id = ?) t " +
            "ORDER BY (dp).path[1], (dp).path[2]";

    private final JdbcTemplate jdbc;

    public GeoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // ---------------- WKT (puros, testeables) ----------------
    public static String wktLinea(List<Punto> pts) {
        return "LINESTRING(" + coords(pts) + ")";
    }

    /** Cierra el anillo si hace falta (primer punto == ultimo). */
    public static String wktPoligono(List<Punto> pts) {
        List<Punto> anillo = new ArrayList<>(pts);
        if (!anillo.get(0).equals(anillo.get(anillo.size() - 1))) anillo.add(anillo.get(0));
        return "POLYGON((" + coords(anillo) + "))";
    }

    private static String coords(List<Punto> pts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pts.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.ROOT, "%.6f %.6f", pts.get(i).lon(), pts.get(i).lat()));   // WKT: X=lon, Y=lat
        }
        return sb.toString();
    }

    // ---------------- rutas ----------------
    public List<Punto> puntosRuta(Long rutaId, Long empresaId) {
        return jdbc.query(DUMP_RUTA, (rs, i) -> new Punto(rs.getDouble("lat"), rs.getDouble("lon")), rutaId, empresaId);
    }

    public List<RutaInfo> rutas(Long empresaId) {
        return jdbc.query("SELECT id FROM ruta WHERE empresa_id = ? ORDER BY nombre",
                (rs, i) -> rs.getLong("id"), empresaId).stream()
                .map(id -> ruta(id, empresaId).orElseThrow()).toList();
    }

    public Optional<RutaInfo> ruta(Long id, Long empresaId) {
        return jdbc.query("SELECT nombre, duracion_plan_min, velocidad_max_kmh FROM ruta WHERE id = ? AND empresa_id = ?",
                (rs, i) -> new RutaInfo(id, rs.getString("nombre"), rs.getInt("duracion_plan_min"),
                        rs.getInt("velocidad_max_kmh"), puntosRuta(id, empresaId)), id, empresaId).stream().findFirst();
    }

    public Long crearRuta(Long empresaId, String nombre, List<Punto> puntos, int duracionPlanMin, int velocidadMaxKmh) {
        try {
            return jdbc.queryForObject(
                    "INSERT INTO ruta (empresa_id, nombre, trazado, duracion_plan_min, velocidad_max_kmh) " +
                    "VALUES (?, ?, ST_GeogFromText(?), ?, ?) RETURNING id",
                    Long.class, empresaId, nombre, wktLinea(puntos), duracionPlanMin, velocidadMaxKmh);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException("Trazado de ruta invalido");
        }
    }

    public boolean existeRuta(Long rutaId, Long empresaId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM ruta WHERE id = ? AND empresa_id = ?", Integer.class, rutaId, empresaId);
        return n != null && n > 0;
    }

    public Optional<Integer> duracionPlanMin(Long rutaId, Long empresaId) {
        return jdbc.query("SELECT duracion_plan_min FROM ruta WHERE id = ? AND empresa_id = ?",
                (rs, i) -> rs.getInt(1), rutaId, empresaId).stream().findFirst();
    }

    /** Metros que faltan para terminar la ruta, medidos SOBRE el trazado desde el punto mas cercano de la ruta. */
    public Optional<Double> distanciaRestanteM(Long rutaId, Long empresaId, double lat, double lon) {
        return jdbc.query(
                "SELECT ST_Length(ST_LineSubstring(g, ST_LineLocatePoint(g, p), 1)::geography) FROM " +
                "(SELECT trazado::geometry AS g, ST_SetSRID(ST_MakePoint(?, ?), 4326) AS p FROM ruta WHERE id = ? AND empresa_id = ?) t",
                (rs, i) -> rs.getDouble(1), lon, lat, rutaId, empresaId).stream().findFirst();
    }

    // ---------------- geocercas ----------------
    public List<GeocercaInfo> geocercas(Long empresaId) {
        return jdbc.query("SELECT id, ruta_id, nombre FROM geocerca WHERE empresa_id = ? ORDER BY id",
                (rs, i) -> new GeocercaInfo(rs.getLong("id"), rs.getLong("ruta_id"), rs.getString("nombre"),
                        jdbc.query(DUMP_GEOCERCA, (r2, j) -> new Punto(r2.getDouble("lat"), r2.getDouble("lon")),
                                rs.getLong("id"), empresaId)), empresaId);
    }

    public Long crearGeocerca(Long empresaId, Long rutaId, String nombre, List<Punto> puntos) {
        try {
            return jdbc.queryForObject(
                    "INSERT INTO geocerca (empresa_id, ruta_id, nombre, area) VALUES (?, ?, ?, ST_GeogFromText(?)) RETURNING id",
                    Long.class, empresaId, rutaId, nombre, wktPoligono(puntos));
        } catch (DataAccessException e) {
            throw new IllegalArgumentException("Poligono de geocerca invalido");
        }
    }

    public boolean eliminarGeocerca(Long id, Long empresaId) {
        return jdbc.update("DELETE FROM geocerca WHERE id = ? AND empresa_id = ?", id, empresaId) > 0;
    }

    /** true si la ruta tiene geocercas y el punto NO esta dentro de ninguna. */
    public boolean fueraDeGeocerca(Long empresaId, Long rutaId, double lat, double lon) {
        return jdbc.queryForObject(
                "SELECT count(*) > 0 AND count(*) FILTER (WHERE ST_Covers(area, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography)) = 0 " +
                "FROM geocerca WHERE empresa_id = ? AND ruta_id = ?",
                Boolean.class, lon, lat, empresaId, rutaId);
    }
}
