# Contratos entre servicios

Fuente de verdad de la API: OpenAPI autogenerado.
- Java (Swagger UI): `http://localhost:8080/swagger-ui.html`
- Python (Swagger UI, solo red interna / dev): `http://localhost:8000/docs`

## Front -> Java (JWT en `Authorization: Bearer ...`)
| Metodo | Ruta | Roles | Descripcion |
|---|---|---|---|
| POST | /api/auth/login | publico | Devuelve token, rol y empresa |
| POST | /api/auth/registro | publico | Crea una empresa nueva y su usuario ADMIN (`empresa`, `nombre`, `email`, `password`); 409 si el email o la empresa ya existen; devuelve la sesion iniciada |
| GET | /api/auth/me | autenticado | Datos de la sesion |
| GET/POST/PUT/DELETE | /api/vehiculos[/{id}] | lectura: todos; escritura: ADMIN | CRUD (baja logica) |
| GET | /api/posiciones/ultimas | ADMIN, OPERADOR, SUPERVISOR | Ultima posicion de cada vehiculo |
| GET | /api/alertas | todos | Ultimas 100 alertas de la empresa |
| PATCH | /api/alertas/{id}/estado | ADMIN, OPERADOR, SUPERVISOR | RECONOCIDA / EN_ATENCION / CERRADA |
| POST | /api/predicciones/eta | ADMIN, OPERADOR, SUPERVISOR | Escenario "que pasaria si" (proxy a Python) |
| POST | /api/predicciones/trayecto | ADMIN, OPERADOR, SUPERVISOR | Riesgo de anomalia de un vehiculo vs una ruta |
| GET | /api/rutas, /api/rutas/{id} | ADMIN, OPERADOR, SUPERVISOR | Rutas con su trazado (lista de puntos lat/lon) |
| POST | /api/rutas | ADMIN, SUPERVISOR | Alta de ruta: nombre, duracionPlanMin, velocidadMaxKmh, puntos (>=2) |
| GET | /api/geocercas | ADMIN, OPERADOR, SUPERVISOR | Corredores autorizados |
| POST / DELETE | /api/geocercas[/{id}] | ADMIN, SUPERVISOR | Alta (poligono, >=3 puntos, se cierra solo) / baja |
| GET | /api/conductores | ADMIN, SUPERVISOR | Datos personales: acceso restringido |
| POST / PUT / DELETE | /api/conductores[/{id}] | ADMIN | CRUD (baja logica) |
| GET / POST / PUT | /api/usuarios[/{id}] | ADMIN | Gestion de usuarios de la empresa (roles, baja, cambio de contrasena); no se puede quitar el rol a uno mismo ni dejar la empresa sin ADMIN |
| GET | /api/viajes | todos | Ultimos 50 viajes de la empresa |
| POST | /api/viajes | ADMIN, SUPERVISOR | Planificar viaje (vehiculoId, rutaId, conductorId opcional) |
| POST | /api/viajes/{id}/iniciar \| finalizar \| cancelar | ADMIN, SUPERVISOR | Ciclo de vida; 409 si la transicion es invalida |
| GET | /api/viajes/{id}/eta | ADMIN, OPERADOR, SUPERVISOR | ETA en vivo: distancia restante sobre la ruta + velocidad reciente |
| GET | /api/viajes/{id}/riesgo | ADMIN, OPERADOR, SUPERVISOR | Puntaje de riesgo del trayecto; >=70 genera alerta |
| GET | /api/modelos/metricas | todos | Metricas de calidad de los modelos (proxy a Python /models/info) |
| GET / POST | /api/modelos/reentrenamiento, /api/modelos/reentrenar | ADMIN | Viajes finalizados utilizables y reentrenamiento del ETA (minimo 20) |
| GET | /api/auditoria | ADMIN, AUDITOR | Bitacora de la empresa |

## Dispositivo GPS -> Java (header `X-Device-Key`)
`POST /api/posiciones`  `{ "ts": "2026-10-12T10:00:00Z", "lat": -34.60, "lon": -58.38, "velocidadKmh": 34.5 }`
Empresa y vehiculo se toman de la credencial del dispositivo, nunca del body.

## Java -> Python (red interna, header `X-Internal-Token`)
`POST /predict/eta`
```json
{ "distancia_restante_km": 15, "velocidad_media_kmh": 30, "hora": 9, "dia_semana": 2, "minutos_ventana_restante": 60 }
```
-> `{ "eta_minutos": 41.2, "intervalo_minutos": [33.0, 49.4], "prob_cumple_ventana": 0.97 }`

`POST /analyze/trayecto`
```json
{ "ruta": [{"lat": -34.60, "lon": -58.38}, ...], "posiciones": [{"ts": "...", "lat": -34.6, "lon": -58.4, "velocidad_kmh": 30}, ...] }
```
-> `{ "puntaje_riesgo": 87.3, "nivel": "CRITICO", "factores": { "vel_media": 12.0, "desvio_max_m": 720.4, ... } }`
Niveles: NORMAL (<40), ALERTA (40-69), CRITICO (>=70). Java crea una alerta `ANOMALIA_TRAYECTO` con riesgo >= 70.

`GET /models/info` -> metricas vigentes (MAE, R2, MAE del baseline ingenuo, falsos positivos y deteccion de anomalias).

`POST /models/retrain` -> reentrena el ETA mezclando datos sinteticos con viajes reales (cada observacion pesa x10).
```json
{ "observaciones": [ { "distancia_km": 15, "velocidad_media_kmh": 30, "hora": 9, "dia_semana": 2, "minutos_reales": 44 } ] }
```
Minimo 20 observaciones. Responde con las metricas del modelo nuevo.

## Reglas de alerta (se evaluan en cada posicion ingestada)
| Alerta | Regla |
|---|---|
| EXCESO_VELOCIDAD | velocidad > limite configurado (80 km/h) |
| PARADA_PROLONGADA | ultimas posiciones con velocidad < 3 km/h durante >= 5 min |
| SALIDA_GEOCERCA | hay viaje EN_CURSO y la posicion esta fuera de todas las geocercas de su ruta |
| ANOMALIA_TRAYECTO | puntaje de riesgo >= 70 (al consultar `/riesgo`) |
Anti-spam: no se repite una alerta NUEVA del mismo tipo y vehiculo en 2 minutos.
