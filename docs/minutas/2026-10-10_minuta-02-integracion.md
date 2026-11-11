# Minuta de reunion - TrackPredict Secure

**Fecha:** 2026-10-XX  **Hora:** HH:MM  **Modalidad:** virtual
**Asistentes:** Eduardo Amaya, Omar Acosta, Diego Rosolen, Jorge Zarate, Martin Herrera Ramirez
**Ausentes:** -

## Objetivo de la reunion
Revisar el avance de los tres servicios, cerrar el contrato Java <-> Python y definir las reglas de alerta y el simulador GPS.

## Temas tratados
1. Contrato entre servicios: endpoints POST /predict/eta y POST /analyze/trayecto con esquemas validados (Pydantic / registros Java) y header X-Internal-Token.
2. Reglas de alerta: exceso de velocidad, parada prolongada, salida de geocerca (ST_Covers) y anomalia de trayecto por puntaje del modelo.
3. Simulador GPS: tres vehiculos, siete rutas, modo anomalia para la demo.
4. Modelos: Gradient Boosting para ETA e Isolation Forest para anomalias, entrenados al iniciar con datos sinteticos.
5. Comunicacion front <-> back: polling cada 4 s en lugar de WebSocket para el MVP.

## Decisiones tomadas
- El servicio de Python no accede a la base; recibe todo en el cuerpo de cada solicitud.
- Solo el nivel CRITICO (riesgo >= 70) genera alerta automatica, para evitar falsos positivos al operador.
- Una alerta nueva del mismo tipo y vehiculo no se repite dentro de los dos minutos siguientes.
- Bitacora de auditoria de solo insercion, protegida por trigger en la base.
- Si Python no responde, Java devuelve 502 con mensaje controlado y el resto sigue operando.

## Acciones asignadas
| Accion | Responsable | Fecha limite | Issue |
|---|---|---|---|
| Implementar AlertaService y pruebas unitarias | [nombre] | AAAA-MM-DD | TPS-# |
| Simulador con modo anomalia y pruebas deterministas | [nombre] | AAAA-MM-DD | TPS-# |
| Mapa Leaflet con rutas, corredores y selector de vehiculo | [nombre] | AAAA-MM-DD | TPS-# |
| Endpoint de metricas GET /models/info y panel de calidad | [nombre] | AAAA-MM-DD | TPS-# |
| docker-compose con healthchecks y orden de arranque | [nombre] | AAAA-MM-DD | TPS-# |

## Bloqueos / riesgos
- La alerta de parada prolongada requiere 5 minutos reales: no se puede mostrar en vivo; queda cubierta por prueba unitaria.
- Formularios web de rutas y geocercas quedan fuera del MVP (alta por Swagger).

## Proxima reunion
Cierre: pruebas de integracion, informe y preparacion de la defensa.
