import { useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api";
import { coloresGrafico, useTema } from "../tema";

interface Punto { hora: number; eta: number }

/** Monitor predictivo: curva de ETA segun la hora de salida, calculada por el microservicio Python (via Java). */
export default function PanelEta({ token }: { token: string }) {
  const [distancia, setDistancia] = useState(15);
  const [velocidad, setVelocidad] = useState(30);
  const [tema] = useTema();
  const c = coloresGrafico(tema);
  const [serie, setSerie] = useState<Punto[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function calcular() {
    setCargando(true);
    setError(null);
    try {
      const diaHabil = 2; // miércoles
      const res = await Promise.all(
        Array.from({ length: 24 }, (_, hora) =>
          api.eta(token, { distanciaRestanteKm: distancia, velocidadMediaKmh: velocidad, hora, diaSemana: diaHabil })
            .then((r) => ({ hora, eta: r.eta_minutos })),
        ),
      );
      setSerie(res);
    } catch {
      setError("El servicio de predicción no respondió. Verificá que esté en ejecución e intentá otra vez.");
    } finally {
      setCargando(false);
    }
  }

  // Lectura de la curva en palabras: mejor y peor hora de salida, y cuánto se pierde por el tránsito.
  const lectura = (() => {
    if (serie.length === 0) return null;
    const mejor = serie.reduce((a, b) => (b.eta < a.eta ? b : a));
    const peor = serie.reduce((a, b) => (b.eta > a.eta ? b : a));
    const sinTransito = (distancia / velocidad) * 60;   // regla simple: distancia / velocidad
    return { mejor, peor, extra: peor.eta - mejor.eta, pct: mejor.eta > 0 ? (peor.eta / mejor.eta - 1) * 100 : 0, sinTransito };
  })();
  const fmt = (x: number) => x.toFixed(1).replace(".", ",");

  return (
    <section className="panel" aria-label="Predicción de ETA">
      <h2>Tiempo de viaje previsto según la hora de salida</h2>
      <div className="fila">
        <label>Distancia (km)<input type="number" min={1} value={distancia} onChange={(e) => setDistancia(Number(e.target.value))} /></label>
        <label>Velocidad media (km/h)<input type="number" min={5} value={velocidad} onChange={(e) => setVelocidad(Number(e.target.value))} /></label>
        <button onClick={calcular} disabled={cargando}>{cargando ? "Calculando…" : "Calcular predicción"}</button>
      </div>
      {error && <p role="alert" className="error">{error}</p>}
      {serie.length === 0 ? (
        <p className="muted">Elegí una distancia y una velocidad para ver cómo cambia el tiempo de viaje a lo largo del día.</p>
      ) : (
        <div style={{ height: 260 }}>
          <ResponsiveContainer>
            <LineChart data={serie} margin={{ top: 10, right: 16, bottom: 10, left: 0 }}>
              <CartesianGrid stroke={c.grilla} />
              <XAxis dataKey="hora" tickFormatter={(h) => `${h}h`} stroke={c.texto} tick={{ fill: c.texto }} />
              <YAxis unit=" min" width={64} stroke={c.texto} tick={{ fill: c.texto }} />
              <Tooltip contentStyle={{ background: c.fondo, border: `1px solid ${c.borde}`, color: c.tinta }} formatter={(v: number) => [`${v} min`, "ETA"]} labelFormatter={(h) => `Salida a las ${h}:00`} />
              <Line type="monotone" dataKey="eta" stroke={c.serie} strokeWidth={2.5} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
      {lectura && (
        <div className="lectura-grafico">
          <p><strong>Cómo leer el gráfico.</strong> Cada punto es el tiempo que tardaría un viaje de {distancia} km a {velocidad} km/h
            según la hora a la que sale (un día hábil). Abajo, las horas del día; a los costados, los minutos de viaje.</p>
          <ul>
            <li>La salida más rápida es a las <strong>{lectura.mejor.hora}:00</strong>, con unos <strong>{fmt(lectura.mejor.eta)} min</strong>.</li>
            <li>La más lenta es a las <strong>{lectura.peor.hora}:00</strong>, con unos <strong>{fmt(lectura.peor.eta)} min</strong>:
              {" "}{fmt(lectura.extra)} min más ({Math.round(lectura.pct)} % más) por el tránsito de hora pico.</li>
            <li>Sin tránsito (distancia / velocidad) serían {fmt(lectura.sinTransito)} min. Las dos subas de la curva son las horas pico de la mañana y de la tarde.</li>
          </ul>
        </div>
      )}
    </section>
  );
}
