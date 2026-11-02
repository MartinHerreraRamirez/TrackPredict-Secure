import { useState } from "react";
import { api } from "../api";
import { puedeGestionarViajes, puedeUsarPredictor } from "../permisos";
import type { EtaViaje, Riesgo, Rol, Ruta, Vehiculo, Viaje } from "../types";

interface Props {
  token: string;
  rol: Rol;
  viajes: Viaje[];
  vehiculos: Vehiculo[];
  rutas: Ruta[];
  onCambio: () => void;
}

interface Prediccion { eta?: EtaViaje; riesgo?: Riesgo; error?: string }

const ESTADOS: Record<string, string> = {
  PLANIFICADO: "planificado", EN_CURSO: "en curso", FINALIZADO: "finalizado", CANCELADO: "cancelado",
};

export default function PanelViajes({ token, rol, viajes, vehiculos, rutas, onCambio }: Props) {
  const [vehiculoId, setVehiculoId] = useState<number | "">("");
  const [rutaId, setRutaId] = useState<number | "">("");
  const [error, setError] = useState<string | null>(null);
  const [predicciones, setPredicciones] = useState<Record<number, Prediccion>>({});

  const patente = (id: number) => vehiculos.find((v) => v.id === id)?.patente ?? `#${id}`;
  const nombreRuta = (id: number) => rutas.find((r) => r.id === id)?.nombre ?? `ruta ${id}`;

  async function planificar() {
    if (vehiculoId === "" || rutaId === "") return;
    try {
      await api.planificarViaje(token, { vehiculoId, rutaId });
      setError(null);
      onCambio();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo planificar el viaje");
    }
  }

  async function accion(id: number, a: "iniciar" | "finalizar" | "cancelar") {
    try {
      await api.accionViaje(token, id, a);
      setError(null);
      onCambio();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo completar la acción");
    }
  }

  async function predecir(id: number) {
    const [eta, riesgo] = await Promise.allSettled([api.etaViaje(token, id), api.riesgoViaje(token, id)]);
    const fallo = [eta, riesgo].find((r) => r.status === "rejected") as PromiseRejectedResult | undefined;
    setPredicciones((p) => ({
      ...p,
      [id]: {
        eta: eta.status === "fulfilled" ? eta.value : undefined,
        riesgo: riesgo.status === "fulfilled" ? riesgo.value : undefined,
        error: fallo ? String(fallo.reason?.message ?? "No se pudo calcular la predicción") : undefined,
      },
    }));
  }

  return (
    <section className="panel" aria-label="Viajes">
      <h2>Viajes</h2>
      {puedeGestionarViajes(rol) && (
        <div className="fila">
          <label>Vehículo
            <select value={vehiculoId} onChange={(e) => setVehiculoId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">Elegí un vehículo</option>
              {vehiculos.map((v) => <option key={v.id} value={v.id}>{v.patente} · {v.tipo.toLowerCase()}</option>)}
            </select>
          </label>
          <label>Ruta
            <select value={rutaId} onChange={(e) => setRutaId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">Elegí una ruta</option>
              {rutas.map((r) => <option key={r.id} value={r.id}>{r.nombre}</option>)}
            </select>
          </label>
          <button onClick={planificar} disabled={vehiculoId === "" || rutaId === ""}>Planificar viaje</button>
        </div>
      )}
      {error && <p role="alert" className="error">{error}</p>}

      {viajes.length === 0 ? (
        <p className="muted">Todavía no hay viajes. Planificá uno eligiendo un vehículo y una ruta.</p>
      ) : (
        <ul className="viajes">
          {viajes.map((v) => {
            const pred = predicciones[v.id];
            return (
              <li key={v.id} data-estado={v.estado}>
                <div className="viaje-cab">
                  <div>
                    <strong>{patente(v.vehiculoId)}</strong> · {nombreRuta(v.rutaId)}
                    <div className="muted">{ESTADOS[v.estado] ?? v.estado}</div>
                  </div>
                  <div className="acciones">
                    {puedeGestionarViajes(rol) && v.estado === "PLANIFICADO" && <button onClick={() => accion(v.id, "iniciar")}>Iniciar</button>}
                    {puedeGestionarViajes(rol) && v.estado === "EN_CURSO" && <button onClick={() => accion(v.id, "finalizar")}>Finalizar</button>}
                    {puedeGestionarViajes(rol) && (v.estado === "PLANIFICADO" || v.estado === "EN_CURSO") &&
                      <button className="ghost" onClick={() => accion(v.id, "cancelar")}>Cancelar</button>}
                    {puedeUsarPredictor(rol) && v.estado === "EN_CURSO" && <button className="ghost" onClick={() => predecir(v.id)}>Ver predicción</button>}
                  </div>
                </div>
                {pred && (
                  <div className="prediccion">
                    {pred.eta && (
                      <p>
                        Llegada estimada en <strong>{pred.eta.eta_minutos.toFixed(0)} min</strong>{" "}
                        (entre {pred.eta.intervalo_minutos[0].toFixed(0)} y {pred.eta.intervalo_minutos[1].toFixed(0)}).
                        Faltan {pred.eta.distancia_restante_km} km a {pred.eta.velocidad_media_kmh} km/h.
                        {pred.eta.prob_cumple_ventana !== null && <> Probabilidad de llegar en ventana: <strong>{Math.round(pred.eta.prob_cumple_ventana * 100)}%</strong>.</>}
                      </p>
                    )}
                    {pred.riesgo && (
                      <div className={pred.riesgo.nivel === "CRITICO" ? "riesgo-critico" : undefined}>
                        {pred.riesgo.nivel === "CRITICO" && <p className="alarma-texto"><span className="sirena" aria-hidden="true" /> ALARMA: trayecto anómalo, riesgo crítico</p>}
                        <div className="barra" role="img" aria-label={`Riesgo ${pred.riesgo.puntaje_riesgo} de 100`}>
                          <span data-nivel={pred.riesgo.nivel} style={{ width: `${pred.riesgo.puntaje_riesgo}%` }} />
                        </div>
                        <p className="muted">Riesgo de trayecto: {pred.riesgo.puntaje_riesgo.toFixed(0)}/100 ({pred.riesgo.nivel.toLowerCase()})</p>
                      </div>
                    )}
                    {pred.error && <p role="alert" className="error">{pred.error}</p>}
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
