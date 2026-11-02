"""Utilidades geograficas sin dependencias externas."""
import math

EARTH_R = 6371000.0


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = p2 - p1
    dl = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * EARTH_R * math.asin(math.sqrt(a))


def _to_xy(lat: float, lon: float, lat0: float) -> tuple[float, float]:
    """Proyeccion equirectangular local (suficiente para distancias urbanas)."""
    x = math.radians(lon) * EARTH_R * math.cos(math.radians(lat0))
    y = math.radians(lat) * EARTH_R
    return x, y


def dist_point_segment_m(p, a, b) -> float:
    lat0 = p[0]
    px, py = _to_xy(p[0], p[1], lat0)
    ax, ay = _to_xy(a[0], a[1], lat0)
    bx, by = _to_xy(b[0], b[1], lat0)
    dx, dy = bx - ax, by - ay
    seg2 = dx * dx + dy * dy
    t = 0.0 if seg2 == 0 else max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / seg2))
    cx, cy = ax + t * dx, ay + t * dy
    return math.hypot(px - cx, py - cy)


def dist_to_polyline_m(point: tuple[float, float], route: list[tuple[float, float]]) -> float:
    """Distancia minima (m) de un punto (lat, lon) a una polilinea de puntos (lat, lon)."""
    if len(route) == 1:
        return haversine_m(point[0], point[1], route[0][0], route[0][1])
    return min(dist_point_segment_m(point, route[i], route[i + 1]) for i in range(len(route) - 1))
