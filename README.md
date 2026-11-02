# TrackPredict Secure

Monitoreo y analitica predictiva de flotas para empresas de seguridad (camiones blindados y autos).
Proyecto Integrador - Programacion de Vanguardia (Lic. en Tecnologias Informaticas, U de la Ciudad de Buenos Aires).

## Arquitectura

```
[Simulador GPS] --X-Device-Key--> [Java Spring Boot :8080] --REST interno--> [Python FastAPI :8000]
                                      |  JWT + RBAC + multi-tenant            (ETA, anomalias)
[React :8081] --JWT--> (nginx /api) --+--> [PostgreSQL + PostGIS]
```

| Carpeta | Servicio | Responsabilidad |
|---|---|---|
| `backend-java/` | Spring Boot 3 / Java 21 | Negocio, auth JWT, RBAC, CRUD, ingesta, alertas, auditoria, gateway hacia Python |
| `analytics-python/` | FastAPI | Prediccion de ETA y deteccion de anomalias de trayecto |
| `frontend/` | React + TypeScript | Login, mapa en vivo, alertas, monitor predictivo, auditoria |
| `simulator/` | Python | Dispositivo GPS simulado (viajes normales y con desvio) |
| `db/init/` | PostgreSQL + PostGIS | Esquema y datos semilla (ficticios) |

El front solo habla con Java. Python no se publica fuera de la red de Compose.

## Como correrlo

```bash
cp .env.example .env            # y cambiar los secretos
docker compose up --build       # db + analytics + backend + frontend + simulador de la flota (se mueven solos)
```
- App: http://localhost:8081  -  API/Swagger: http://localhost:8080/swagger-ui.html

Usuarios de ejemplo (solo desarrollo): `admin@trackpredict.com / Admin123!`, `operador@trackpredict.com / Operador123!`,
`auditor@trackpredict.com / Audit123!`, `admin@norte.com / Super123!` (otra empresa, para probar el aislamiento).
Tambien se puede crear una cuenta propia desde la pantalla de ingreso ("Crear cuenta"): registra una empresa nueva y su administrador.
Claves de los dispositivos de demo: `demo-device-key-001`, `-002` y `-003` (una por vehiculo de Seguridad Demo).

## Scripts para Windows (doble clic)
`iniciar.bat` levanta todo y abre el navegador; `detener.bat` apaga todo conservando los datos; `reiniciar-limpio.bat` borra la base y empieza de cero;
`demo-anomalia.bat` provoca el desvio de la escolta. Requieren Docker Desktop abierto.

## Flujo de demo
1. Levantar todo con `docker compose up --build` (o doble clic en `iniciar.bat` en Windows). El simulador arranca solo, inicia los 3 viajes
   planificados (blindado AB123CD, auto AC456EF y escolta AD789GH) y empieza a mover los vehiculos.
2. Ingresar como `admin@trackpredict.com`: el mapa muestra los 3 vehiculos recorriendo sus rutas, cada una dentro de su corredor punteado. Cada vehiculo empieza por un trayecto largo (Ezeiza, Tigre o La Plata, de 13 a 19 min en la demo) y despues alterna con rutas cortas. La base trae 24 viajes finalizados de historial (ficticios), asi que el boton de reentrenar ya esta habilitado. El selector "Ver" del mapa muestra un solo vehiculo (su ruta, alertas y viajes).
3. **Ver prediccion** en cualquier viaje: ETA con intervalo, probabilidad de llegar en ventana y barra de riesgo (bajo).
4. Para forzar una anomalia en un vehiculo: doble clic en `demo-anomalia.bat` (pausa el simulador, desvia a AD789GH y lo retoma). La escolta se desvia ~700 m y se detiene: aparecen la alerta de salida de geocerca y, al consultar
   la prediccion, la de anomalia de trayecto (riesgo critico).
5. Ingresar como `admin@norte.com`: ve solo su vehiculo (AE321KL) y ninguno de Seguridad Demo (aislamiento entre empresas).
6. Ingresar como `auditor@trackpredict.com`: solo ve alertas, viajes y bitacora (sin mapa ni acciones).

Nota: si ya habias levantado el hito 1, recrear la base (`docker compose down -v`) para aplicar el esquema nuevo.

## Calidad de los modelos (evidencia para el informe)
Con `GET /models/info` (datos sinteticos, conjunto de prueba aparte):
- ETA: MAE ~4,0 min frente a ~6,5 min del baseline ingenuo (distancia / velocidad), R2 ~0,98.
- Anomalias: deteccion >= 99% a nivel alerta y ~72% a nivel critico; falsos positivos ~8,5% a nivel alerta.
Son cifras sobre datos sinteticos: sirven para comparar versiones del modelo, no para prometer precision en campo.

## Pruebas
```bash
cd analytics-python && pip install -r requirements.txt && pytest      # ETA, anomalias, token interno
cd simulator && pip install -r requirements.txt && pytest             # generador de viajes
cd frontend && npm ci && npm test && npm run build                    # permisos por rol + compilacion TS
cd backend-java && mvn test                                           # JWT y reglas de alerta
```

## Gestion del proyecto
`docs/GITFLOW.md` (ramas, PRs, Kanban, epicas), `docs/contratos.md` (API), `docs/minutas/`, `.github/` (plantillas).

## Equipo y gestión del proyecto
Integrantes: Eduardo Amaya, Omar Acosta, Diego Rosolen, Jorge Zárate y Martin Herrera Ramirez.
Docente: Ing. Alejandro Vázquez.

- Tablero de tareas (Jira): https://martinherreraramirez2016.atlassian.net/jira/software/projects/TPS/boards/2
- Flujo de ramas: `docs/GITFLOW.md` · Contratos de API: `docs/contratos.md`
