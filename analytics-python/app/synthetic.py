"""Generador de datos sinteticos de entrenamiento (no se usan datos reales de flotas)."""
import numpy as np

RNG_SEED = 42


def traffic_factor(hora: np.ndarray, dia: np.ndarray) -> np.ndarray:
    """Factor multiplicativo de demora: picos 8-10 y 17-19 en dias habiles."""
    pico = np.exp(-((hora - 9) ** 2) / 3.0) + np.exp(-((hora - 18) ** 2) / 3.0)
    habil = (dia < 5).astype(float)
    return 1.0 + 0.45 * pico * habil + 0.05 * (1 - habil)


def eta_dataset(n: int = 6000):
    rng = np.random.default_rng(RNG_SEED)
    dist = rng.uniform(0.5, 60, n)
    vel = rng.uniform(15, 70, n)
    hora = rng.integers(0, 24, n)
    dia = rng.integers(0, 7, n)
    base = dist / vel * 60
    y = base * traffic_factor(hora, dia) + rng.normal(0, 1.5 + 0.04 * base, n)
    X = np.column_stack([dist, vel, hora, dia])
    return X, np.clip(y, 0.5, None)


def trayecto_features_normales(n: int = 4000) -> np.ndarray:
    """Columnas: vel_media, vel_std, desvio_max_m, ratio_detenido, ratio_duracion."""
    rng = np.random.default_rng(RNG_SEED + 1)
    vel_media = rng.normal(34, 5, n)
    vel_std = np.abs(rng.normal(8, 2, n))
    desvio = np.abs(rng.normal(25, 12, n))
    detenido = np.clip(rng.beta(1.5, 20, n), 0, 1)
    ratio_dur = rng.normal(1.0, 0.1, n)
    return np.column_stack([vel_media, vel_std, desvio, detenido, ratio_dur])


def trayecto_features_anomalos(n: int = 600) -> np.ndarray:
    """Tres familias de comportamiento anomalo: desvio de ruta, parada prolongada y velocidad inusual."""
    rng = np.random.default_rng(RNG_SEED + 2)
    base = trayecto_features_normales(n)
    kind = rng.integers(0, 3, n)
    out = base.copy()
    d = kind == 0                                  # desvio
    out[d, 2] = rng.uniform(400, 1500, d.sum())
    p = kind == 1                                  # parada prolongada
    out[p, 3] = rng.uniform(0.4, 0.9, p.sum())
    out[p, 0] = rng.uniform(4, 15, p.sum())
    v = kind == 2                                  # velocidad inusual
    out[v, 0] = rng.uniform(65, 110, v.sum())
    out[v, 1] = rng.uniform(18, 30, v.sum())
    return out
