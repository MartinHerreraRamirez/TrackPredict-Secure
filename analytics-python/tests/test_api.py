from datetime import datetime, timedelta, timezone

from fastapi.testclient import TestClient

from app.main import app

H = {"X-Internal-Token": "dev-internal-token"}
RUTA = [{"lat": -34.6037, "lon": -58.3816}, {"lat": -34.6080, "lon": -58.3700},
        {"lat": -34.6130, "lon": -58.3640}, {"lat": -34.6200, "lon": -58.3620}]


def trip(desvio_deg=0.0, vel=34.0, n=30):
    t0 = datetime(2026, 10, 12, 10, 0, tzinfo=timezone.utc)
    out = []
    for i in range(n):
        f = i / (n - 1) * 3
        k = min(int(f), 2)
        a, b = RUTA[k], RUTA[k + 1]
        lat = a["lat"] + (b["lat"] - a["lat"]) * (f - k)
        lon = a["lon"] + (b["lon"] - a["lon"]) * (f - k)
        shift = desvio_deg if 10 < i < 20 else 0.0
        out.append({"ts": (t0 + timedelta(seconds=30 * i)).isoformat(),
                    "lat": lat + shift, "lon": lon + shift, "velocidad_kmh": max(0.0, vel + (8.0 * (1 if i % 2 else -1) if vel > 10 else 0.0))})
    return out


def test_health():
    with TestClient(app) as c:
        assert c.get("/health").json()["status"] == "ok"


def test_requires_internal_token():
    with TestClient(app) as c:
        r = c.post("/predict/eta", json={"distancia_restante_km": 5, "velocidad_media_kmh": 30,
                                         "hora": 10, "dia_semana": 1})
        assert r.status_code == 401


def test_eta_is_reasonable_and_rush_hour_is_slower():
    with TestClient(app) as c:
        body = {"distancia_restante_km": 15, "velocidad_media_kmh": 30, "hora": 3, "dia_semana": 2,
                "minutos_ventana_restante": 60}
        night = c.post("/predict/eta", json=body, headers=H).json()
        body["hora"] = 9
        rush = c.post("/predict/eta", json=body, headers=H).json()
        assert 25 < night["eta_minutos"] < 40          # 15 km a 30 km/h = 30 min
        assert rush["eta_minutos"] > night["eta_minutos"]
        lo, hi = night["intervalo_minutos"]
        assert lo < night["eta_minutos"] < hi
        assert 0.9 < night["prob_cumple_ventana"] <= 1.0


def test_normal_trip_has_low_risk_and_deviation_is_flagged():
    with TestClient(app) as c:
        normal = c.post("/analyze/trayecto", json={"ruta": RUTA, "posiciones": trip()}, headers=H).json()
        desviado = c.post("/analyze/trayecto", json={"ruta": RUTA, "posiciones": trip(desvio_deg=0.01, vel=2.0)},
                          headers=H).json()
        assert normal["puntaje_riesgo"] < 40
        assert desviado["puntaje_riesgo"] >= 70 and desviado["nivel"] == "CRITICO"


def test_modelo_eta_supera_al_baseline_ingenuo():
    with TestClient(app) as c:
        m = c.get("/models/info", headers=H).json()["eta"]
        assert m["mae_min"] < m["mae_baseline_min"]     # el modelo aprende el efecto del trafico
        assert m["r2"] > 0.9


def test_deteccion_de_anomalias_cumple_umbrales_de_calidad():
    with TestClient(app) as c:
        m = c.get("/models/info", headers=H).json()["anomalias"]
        assert m["falsos_positivos_alerta"] < 0.10
        assert m["deteccion_anomalias_alerta"] > 0.85


def test_reentrenamiento_incorpora_observaciones_reales():
    with TestClient(app) as c:
        body = {"distancia_restante_km": 15, "velocidad_media_kmh": 30, "hora": 3, "dia_semana": 2}
        antes = c.post("/predict/eta", json=body, headers=H).json()["eta_minutos"]
        # En la realidad ese tramo tarda el doble de lo que dice el modelo sintetico
        obs = [{"distancia_km": 15, "velocidad_media_kmh": 30, "hora": 3, "dia_semana": 2, "minutos_reales": 60}] * 60
        r = c.post("/models/retrain", json={"observaciones": obs}, headers=H).json()
        despues = c.post("/predict/eta", json=body, headers=H).json()["eta_minutos"]
        assert r["metricas"]["observaciones_reales"] == 60
        assert despues > antes + 10


def test_reentrenar_exige_minimo_de_observaciones():
    with TestClient(app) as c:
        obs = [{"distancia_km": 5, "velocidad_media_kmh": 30, "hora": 3, "dia_semana": 2, "minutos_reales": 10}]
        assert c.post("/models/retrain", json={"observaciones": obs}, headers=H).status_code == 422
