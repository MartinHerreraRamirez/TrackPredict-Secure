# Minuta de reunion - TrackPredict Secure

**Fecha:** 2026-10-XX  **Hora:** HH:MM  **Modalidad:** virtual
**Asistentes:** Eduardo Amaya, Omar Acosta, Diego Rosolen, Jorge Zarate, Martin Herrera Ramirez
**Ausentes:** -

## Objetivo de la reunion
Definir la problematica, el alcance del MVP y el stack tecnologico para la propuesta de la Fase 1.

## Temas tratados
1. Eleccion del dominio: monitoreo de flotas para empresas de seguridad y transporte de caudales. Se descartaron alternativas de gestion universitaria por menor valor diferencial.
2. Rol del modulo predictivo: estimacion de tiempo de llegada (ETA) y deteccion de anomalias de trayecto.
3. Stack: Java 21 + Spring Boot 3 para el nucleo de negocio; Python 3.12 + FastAPI + scikit-learn para la analitica; React + TypeScript + Leaflet para el front-end.
4. Base de datos: comparacion entre PostgreSQL + PostGIS, MongoDB y TimescaleDB.
5. Modelo multi-tenant: una sola base con columna empresa_id en todas las tablas.

## Decisiones tomadas
- Java es el unico punto de entrada; el servicio de Python no se publica fuera de la red interna.
- Se adopta PostgreSQL 16 + PostGIS como unico motor (relacional + geoespacial).
- Cuatro roles: Administrador, Operador, Supervisor y Auditor, con autenticacion JWT.
- Los datos de desarrollo y demo seran 100% sinteticos; no se usan rutas ni flotas reales.
- Gestion del proyecto en Jira (clave TPS) y repositorio en GitHub.

## Acciones asignadas
| Accion | Responsable | Fecha limite | Issue |
|---|---|---|---|
| Redactar propuesta de Fase 1 y mockups preliminares | [nombre] | AAAA-MM-DD | TPS-# |
| Esqueleto Spring Boot con JWT y RBAC | [nombre] | AAAA-MM-DD | TPS-# |
| Esqueleto FastAPI con endpoint /health y contrato /predict/eta | [nombre] | AAAA-MM-DD | TPS-# |
| Esquema SQL inicial (db/init) con PostGIS | [nombre] | AAAA-MM-DD | TPS-# |
| Crear tablero Jira con epicas y repositorio | Martin Herrera Ramirez | AAAA-MM-DD | TPS-# |

## Bloqueos / riesgos
- Sin datos reales: las metricas de los modelos solo serviran para comparar versiones, no para afirmar precision en campo.
- Cinco integrantes con disponibilidad horaria dispar; se coordina por mensajeria entre reuniones.

## Proxima reunion
Revision de la integracion Java <-> Python y de las reglas de alerta.
