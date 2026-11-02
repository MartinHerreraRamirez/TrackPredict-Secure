"""Microservicio analitico (FastAPI). Solo accesible desde la red interna, via el servicio Java."""
import os
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, Header, HTTPException

from .models import AnomalyModel, ETAModel, Observacion, extract_features
from .schemas import ETARequest, ETAResponse, ReentrenarRequest, TrayectoRequest, TrayectoResponse

INTERNAL_TOKEN = os.getenv("INTERNAL_TOKEN", "dev-internal-token")
state: dict = {}


@asynccontextmanager
async def lifespan(_: FastAPI):
    state["eta"] = ETAModel()
    state["anom"] = AnomalyModel()
    yield


app = FastAPI(title="TrackPredict Analytics", version="0.1.0", lifespan=lifespan)


def require_internal_token(x_internal_token: str = Header(default="")) -> None:
    if x_internal_token != INTERNAL_TOKEN:
        raise HTTPException(status_code=401, detail="Token interno invalido")


@app.get("/health")
def health():
    return {"status": "ok", "eta_mae_min": round(state["eta"].mae, 2)}


@app.post("/predict/eta", response_model=ETAResponse, dependencies=[Depends(require_internal_token)])
def predict_eta(req: ETARequest):
    eta, (lo, hi), prob = state["eta"].predict(
        req.distancia_restante_km, req.velocidad_media_kmh, req.hora, req.dia_semana,
        req.minutos_ventana_restante)
    return ETAResponse(eta_minutos=round(eta, 1), intervalo_minutos=(round(lo, 1), round(hi, 1)),
                       prob_cumple_ventana=None if prob is None else round(prob, 3))


@app.post("/analyze/trayecto", response_model=TrayectoResponse, dependencies=[Depends(require_internal_token)])
def analyze_trayecto(req: TrayectoRequest):
    ruta = [(p.lat, p.lon) for p in req.ruta]
    pos = [p.model_dump() for p in req.posiciones]
    feats = extract_features(ruta, pos)
    riesgo = state["anom"].score(feats)
    nivel = "CRITICO" if riesgo >= 70 else "ALERTA" if riesgo >= 40 else "NORMAL"
    factores = dict(zip(state["anom"].COLS, (round(float(v), 2) for v in feats)))
    return TrayectoResponse(puntaje_riesgo=round(riesgo, 1), nivel=nivel, factores=factores)


@app.get("/models/info", dependencies=[Depends(require_internal_token)])
def models_info():
    """Metricas de calidad de los modelos vigentes (evidencia de QA)."""
    return {"eta": state["eta"].metrics, "anomalias": state["anom"].metrics}


@app.post("/models/retrain", dependencies=[Depends(require_internal_token)])
def retrain(req: ReentrenarRequest):
    """Reentrena el modelo de ETA mezclando datos sinteticos con viajes reales finalizados."""
    obs = [Observacion(**o.model_dump()) for o in req.observaciones]
    nuevo = ETAModel(observaciones=obs)
    state["eta"] = nuevo          # reemplazo atomico: las predicciones en curso no se interrumpen
    return {"mensaje": "Modelo de ETA reentrenado", "metricas": nuevo.metrics}
