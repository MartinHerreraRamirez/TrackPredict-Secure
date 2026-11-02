# Flujo de trabajo Git (GitFlow)

| Rama | Proposito | Se integra a |
|---|---|---|
| `main` | Versiones estables entregables | - |
| `develop` | Integracion continua del equipo | `main` (via `release/*`) |
| `feature/<epica>-<tema>` | Una historia de usuario | `develop` (PR) |
| `release/x.y` | Estabilizacion antes de entregar | `main` y `develop` |
| `hotfix/<tema>` | Correccion urgente sobre `main` | `main` y `develop` |

Reglas:
1. Nadie hace push directo a `main` ni a `develop`: todo entra por Pull Request.
2. Cada PR necesita la revision de **otra** persona del equipo y los tests en verde.
3. Cada PR referencia su issue (`Closes #12`). Commits en formato `tipo(alcance): mensaje` (feat, fix, docs, test, chore).
4. Tablero Kanban: Backlog -> Por hacer -> En curso -> En revision -> Hecho.
5. Congelamiento de funcionalidades: 27/10. Despues solo fixes y documentacion.

## Epicas
1. Autenticacion, RBAC y multi-tenant  2. Flota, rutas y geocercas  3. Ingesta y simulador GPS
4. Reglas de alerta e incidentes  5. Modulo predictivo (ETA, anomalias)  6. Dashboard
7. Auditoria  8. Pruebas, documentacion y despliegue
