from datetime import datetime
from pydantic import BaseModel, Field


class ETARequest(BaseModel):
    distancia_restante_km: float = Field(ge=0, le=2000)
    velocidad_media_kmh: float = Field(gt=0, le=200)
    hora: int = Field(ge=0, le=23)
    dia_semana: int = Field(ge=0, le=6, description="0=lunes ... 6=domingo")
    minutos_ventana_restante: float | None = Field(
        default=None, description="Minutos que faltan para cerrar la ventana horaria")


class ETAResponse(BaseModel):
    eta_minutos: float
    intervalo_minutos: tuple[float, float]
    prob_cumple_ventana: float | None = None


class Punto(BaseModel):
    lat: float = Field(ge=-90, le=90)
    lon: float = Field(ge=-180, le=180)


class PosicionIn(Punto):
    ts: datetime
    velocidad_kmh: float = Field(ge=0, le=300)


class TrayectoRequest(BaseModel):
    ruta: list[Punto] = Field(min_length=2)
    posiciones: list[PosicionIn] = Field(min_length=3)


class TrayectoResponse(BaseModel):
    puntaje_riesgo: float
    nivel: str
    factores: dict[str, float]


class ObservacionIn(BaseModel):
    distancia_km: float = Field(gt=0, le=2000)
    velocidad_media_kmh: float = Field(gt=0, le=200)
    hora: int = Field(ge=0, le=23)
    dia_semana: int = Field(ge=0, le=6)
    minutos_reales: float = Field(gt=0, le=2000)


class ReentrenarRequest(BaseModel):
    observaciones: list[ObservacionIn] = Field(min_length=20, description="Viajes finalizados con su duracion real")
