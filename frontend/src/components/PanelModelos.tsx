import { useEffect, useState } from "react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api";
import { coloresGrafico, useTema } from "../tema";
import { puedeReentrenarModelos } from "../permisos";
import type { EstadoReentrenamiento, MetricasModelos, Rol } from "../types";

const pct = (x: number) => `${(x * 100).toFixed(1).replace(".", ",")} %`;
const num = (x: number, d = 2) => x.toFixed(d).replace(".", ",");

function Kpi({ valor, rotulo }: { valor: string; rotulo: string }) {
  return (
    <div className="kpi">
      <div className="valor">{valor}</div>
      <div className="rotulo">{rotulo}</div>
    </div>
  );
}

/** Calidad de los modelos predictivos: las metricas las calcula el servicio Python y llegan via Java. */
export default function PanelModelos({ token, rol }: { token: string; rol: Rol }) {
  const [tema] = useTema();
  const c = coloresGrafico(tema);
  const [m, setM] = useState<MetricasModelos | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [estado, setEstado] = useState<EstadoReentrenamiento | null>(null);
  const [confirmando, setConfirmando] = useState(false);
  const [trabajando, setTrabajando] = useState(false);
  const [resultado, setResultado] = useState<string | null>(null);
  const [errorRe, setErrorRe] = useState<string | null>(null);

  useEffect(() => {
    api.metricasModelos(token).then(setM).catch(() => setError("No se pudieron obtener las métricas. Verificá que el servicio de predicción esté en ejecución."));
    if (puedeReentrenarModelos(rol)) api.estadoReentrenamiento(token).then(setEstado).catch(() => undefined);
  }, [token, rol]);

  async function reentrenar() {
    if (!m) return;
    const antes = m.eta.mae_min;
    setTrabajando(true);
    setErrorRe(null);
    setResultado(null);
    try {
      const r = await api.reentrenar(token);
      setM({ ...m, eta: r.metricas });
      setResultado(`Modelo reentrenado con ${r.metricas.observaciones_reales} viajes reales. Error medio: ${num(antes)} min antes, ${num(r.metricas.mae_min)} min ahora.`);
    } catch (e) {
      setErrorRe(e instanceof Error ? e.message : "No se pudo reentrenar el modelo");
    } finally {
      setTrabajando(false);
      setConfirmando(false);
    }
  }

  if (error) return <section className="panel" aria-label="Calidad de los modelos"><h2>Calidad de los modelos predictivos</h2><p role="alert" className="error">{error}</p></section>;
  if (!m) return <section className="panel" aria-label="Calidad de los modelos"><h2>Calidad de los modelos predictivos</h2><p className="muted">Cargando métricas…</p></section>;

  const { eta, anomalias } = m;
  const mejora = eta.mae_baseline_min > 0 ? 1 - eta.mae_min / eta.mae_baseline_min : 0;
  const comparacion = [
    { modelo: "Regla simple (distancia / velocidad)", error: eta.mae_baseline_min },
    { modelo: "Modelo de ETA", error: eta.mae_min },
  ];

  return (
    <section className="panel" aria-label="Calidad de los modelos">
      <h2>Calidad de los modelos predictivos</h2>
      <p className="muted">
        Métricas medidas sobre un conjunto de prueba separado del entrenamiento. Datos de entrenamiento: {eta.muestras_entrenamiento.toLocaleString("es-AR")} muestras sintéticas
        {eta.observaciones_reales > 0 ? ` y ${eta.observaciones_reales} observaciones reales` : " (todavía sin observaciones reales)"}.
      </p>
      <div className="modelos-grid">
        <div>
          <h3>Tiempo de llegada (ETA)</h3>
          <div className="kpis">
            <Kpi valor={`${num(eta.mae_min)} min`} rotulo="Error medio absoluto (MAE)" />
            <Kpi valor={`${num(eta.rmse_min)} min`} rotulo="Raíz del error cuadrático (RMSE)" />
            <Kpi valor={num(eta.r2, 3)} rotulo="R² (1 es ajuste perfecto)" />
            <Kpi valor={pct(mejora)} rotulo="Menos error que la regla simple" />
          </div>
          <div style={{ height: 200 }}>
            <ResponsiveContainer>
              <BarChart data={comparacion} layout="vertical" margin={{ top: 5, right: 24, bottom: 5, left: 8 }}>
                <CartesianGrid stroke={c.grilla} horizontal={false} />
                <XAxis type="number" unit=" min" stroke={c.texto} tick={{ fill: c.texto }} />
                <YAxis type="category" dataKey="modelo" width={190} stroke={c.texto} tick={{ fontSize: 12, fill: c.texto }} />
                <Tooltip cursor={{ fill: c.grilla, opacity: 0.4 }} contentStyle={{ background: c.fondo, border: `1px solid ${c.borde}`, color: c.tinta }} formatter={(v: number) => [`${num(v)} min`, "Error medio"]} />
                <Bar dataKey="error" fill={c.serie} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div>
          <h3>Anomalías de trayecto</h3>
          <div className="kpis">
            <Kpi valor={pct(anomalias.deteccion_anomalias_alerta)} rotulo="Anomalías detectadas (nivel alerta o más)" />
            <Kpi valor={pct(anomalias.deteccion_anomalias_critico)} rotulo="Anomalías que llegan a crítico" />
            <Kpi valor={pct(anomalias.falsos_positivos_alerta)} rotulo="Falsas alarmas en viajes normales" />
          </div>
          <p className="muted">
            El puntaje de riesgo va de 0 a 100: normal por debajo de 40, alerta de 40 a 69 y crítico desde 70, nivel en el que se genera una alerta automática.
          </p>
        </div>
      </div>

      {puedeReentrenarModelos(rol) && estado && (
        <div style={{ marginTop: "1rem", borderTop: "1px solid var(--line)", paddingTop: "1rem" }}>
          <h3>Reentrenar con viajes reales</h3>
          <p className="muted">
            El modelo de ETA aprende de los viajes finalizados de tu empresa (distancia, velocidad media, hora, día y duración real).
            Viajes utilizables: {estado.viajesDisponibles} de {estado.minimoRequerido} necesarios. El reentrenamiento cambia el modelo para
            todas las empresas y se pierde si el servicio de predicción se reinicia.
          </p>
          {estado.viajesDisponibles < estado.minimoRequerido && (
            <p role="status" className="banner">
              Botón deshabilitado: faltan {estado.minimoRequerido - estado.viajesDisponibles} viajes finalizados.
              Se habilita solo cuando hay {estado.minimoRequerido} o más; con menos datos reales el modelo no mejoraría.
            </p>
          )}
          <div className="fila">
            {!confirmando ? (
              <button onClick={() => setConfirmando(true)} disabled={trabajando || estado.viajesDisponibles < estado.minimoRequerido}>
                Reentrenar modelo de ETA
              </button>
            ) : (
              <>
                <button onClick={reentrenar} disabled={trabajando}>{trabajando ? "Reentrenando…" : "Confirmar reentrenamiento"}</button>
                <button className="ghost" onClick={() => setConfirmando(false)} disabled={trabajando}>Cancelar</button>
              </>
            )}
          </div>
          {resultado && <p role="status">{resultado}</p>}
          {errorRe && <p role="alert" className="error">{errorRe}</p>}
        </div>
      )}
    </section>
  );
}
