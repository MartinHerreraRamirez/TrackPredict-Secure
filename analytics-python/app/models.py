"""Modelos predictivos: ETA (Gradient Boosting) y anomalias de trayecto (Isolation Forest).

Ambos se entrenan con datos sinteticos y exponen metricas de calidad (para QA y defensa).
El ETA puede reentrenarse mezclando observaciones reales (viajes finalizados) con peso mayor.
"""
import math
from dataclasses import dataclass

import numpy as np
from sklearn.ensemble import GradientBoostingRegressor, IsolationForest

from . import synthetic
from .geo import dist_to_polyline_m

REAL_WEIGHT = 10.0   # cada observacion real pesa 10 veces mas que una sintetica


@dataclass
class Observacion:
    distancia_km: float
    velocidad_media_kmh: float
    hora: int
    dia_semana: int
    minutos_reales: float


class ETAModel:
    def __init__(self, observaciones: list[Observacion] | None = None) -> None:
        X, y = synthetic.eta_dataset()
        split = int(len(X) * 0.8)
        Xtr, ytr, Xte, yte = X[:split], y[:split], X[split:], y[split:]
        w = np.ones(len(Xtr))
        n_real = 0
        if observaciones:
            Xr = np.array([[o.distancia_km, o.velocidad_media_kmh, o.hora, o.dia_semana] for o in observaciones])
            yr = np.array([o.minutos_reales for o in observaciones])
            Xtr, ytr = np.vstack([Xtr, Xr]), np.concatenate([ytr, yr])
            w = np.concatenate([w, np.full(len(yr), REAL_WEIGHT)])
            n_real = len(yr)
        self.model = GradientBoostingRegressor(n_estimators=200, max_depth=3, random_state=0)
        self.model.fit(Xtr, ytr, sample_weight=w)

        pred = self.model.predict(Xte)
        residuals = yte - pred
        baseline = Xte[:, 0] / Xte[:, 1] * 60          # ingenuo: distancia / velocidad, sin trafico
        self.sigma_rel = float(np.std(residuals / np.clip(yte, 1, None)))
        self.mae = float(np.mean(np.abs(residuals)))
        ss_res, ss_tot = float(np.sum(residuals ** 2)), float(np.sum((yte - yte.mean()) ** 2))
        self.metrics = {
            "mae_min": round(self.mae, 3),
            "rmse_min": round(float(np.sqrt(np.mean(residuals ** 2))), 3),
            "r2": round(1 - ss_res / ss_tot, 4),
            "mae_baseline_min": round(float(np.mean(np.abs(yte - baseline))), 3),
            "muestras_entrenamiento": int(len(ytr)),
            "observaciones_reales": n_real,
        }

    def predict(self, dist_km, vel_kmh, hora, dia, minutos_ventana=None):
        x = np.array([[dist_km, vel_kmh, hora, dia]])
        eta = float(self.model.predict(x)[0])
        sigma = max(1.0, self.sigma_rel * eta)
        interval = (max(0.0, eta - 1.645 * sigma), eta + 1.645 * sigma)
        prob = None
        if minutos_ventana is not None:
            z = (minutos_ventana - eta) / sigma
            prob = 0.5 * (1 + math.erf(z / math.sqrt(2)))
        return eta, interval, prob


class AnomalyModel:
    COLS = ["vel_media", "vel_std", "desvio_max_m", "ratio_detenido", "ratio_duracion"]
    UMBRAL_ALERTA, UMBRAL_CRITICO = 40.0, 70.0

    def __init__(self) -> None:
        X = synthetic.trayecto_features_normales()
        self.mu, self.sd = X.mean(axis=0), X.std(axis=0)
        self.model = IsolationForest(n_estimators=200, contamination=0.01, random_state=0)
        self.model.fit((X - self.mu) / self.sd)
        raw = self._raw(X)
        self.p50, self.p99 = float(np.percentile(raw, 50)), float(np.percentile(raw, 99))
        self.metrics = self._evaluar()

    def _raw(self, X: np.ndarray) -> np.ndarray:
        return -self.model.score_samples((X - self.mu) / self.sd)

    def score_batch(self, X: np.ndarray) -> np.ndarray:
        risk = (self._raw(X) - self.p50) / (self.p99 - self.p50) * 70.0
        return np.clip(risk, 0, 100)

    def score(self, feats: np.ndarray) -> float:
        return float(self.score_batch(feats.reshape(1, -1))[0])

    def _evaluar(self) -> dict:
        """Evalua con viajes normales NUEVOS y con anomalias sinteticas etiquetadas."""
        rng = np.random.default_rng(999)
        normales = synthetic.trayecto_features_normales(2000) * rng.normal(1.0, 0.01, (2000, 5))
        anomalos = synthetic.trayecto_features_anomalos(600)
        sn, sa = self.score_batch(normales), self.score_batch(anomalos)
        return {
            "falsos_positivos_alerta": round(float(np.mean(sn >= self.UMBRAL_ALERTA)), 4),
            "deteccion_anomalias_alerta": round(float(np.mean(sa >= self.UMBRAL_ALERTA)), 4),
            "deteccion_anomalias_critico": round(float(np.mean(sa >= self.UMBRAL_CRITICO)), 4),
        }


def extract_features(ruta: list[tuple[float, float]], posiciones: list[dict], duracion_plan_min: float | None = None):
    pos = sorted(posiciones, key=lambda p: p["ts"])
    speeds = np.array([p["velocidad_kmh"] for p in pos], dtype=float)
    desvios = [dist_to_polyline_m((p["lat"], p["lon"]), ruta) for p in pos]
    dur_min = (pos[-1]["ts"] - pos[0]["ts"]).total_seconds() / 60 if len(pos) > 1 else 0.0
    ratio_dur = dur_min / duracion_plan_min if duracion_plan_min else 1.0
    return np.array([
        speeds.mean(), speeds.std(), max(desvios), float(np.mean(speeds < 3.0)), ratio_dur
    ])
