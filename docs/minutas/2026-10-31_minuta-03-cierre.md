# Minuta de reunion - TrackPredict Secure

**Fecha:** 2026-10-31  **Hora:** HH:MM  **Modalidad:** virtual
**Asistentes:** Eduardo Amaya, Omar Acosta, Diego Rosolen, Jorge Zarate, Martin Herrera Ramirez
**Ausentes:** -

## Objetivo de la reunion
Ejecutar las pruebas de integracion sobre el sistema completo, cerrar el informe tecnico y organizar la presentacion.

## Temas tratados
1. Ejecucion de las 50 pruebas unitarias (25 Java, 8 Python, 7 simulador, 10 front-end) y de los 9 casos de integracion IT-01 a IT-09 con Docker Compose.
2. Limitaciones de seguridad a declarar en el informe (sin TLS local, sin limite de intentos, sin revocacion de tokens).
3. Estructura del informe tecnico y de la presentacion; reparto de la defensa.
4. Estado del repositorio: el desarrollo se consolido en la rama main; el flujo GitFlow queda documentado.

## Decisiones tomadas
- Las 59 pruebas pasan; se registran los resultados en la seccion 9 del informe.
- Las limitaciones se declaran explicitamente en las secciones 6.4 y 12.
- Entrega final: informe PDF, presentacion y repositorio, el 12 de noviembre.
- La demo en vivo la realiza un solo integrante; el resto expone las slides.

## Acciones asignadas
| Accion | Responsable | Fecha limite | Issue |
|---|---|---|---|
| Publicar el repositorio y cargar el tablero Jira | Martin Herrera Ramirez | 2026-11-01 | TPS-# |
| Cerrar el informe tecnico v2.0 | [nombre] | 2026-11-10 | TPS-# |
| Presentacion y guion de la defensa | [nombre] | 2026-11-10 | TPS-32 |
| Minutas de reunion en docs/minutas | Diego Rosolen | 2026-11-10 | TPS-# |

## Bloqueos / riesgos
- El historial de ramas y pull requests no refleja el desarrollo; se declara en la seccion 11.2 del informe.
- Dependencia de Docker Desktop en la sala: se prepara plan B con capturas y video.

## Proxima reunion
Ensayo de la defensa antes del 12 de noviembre.
