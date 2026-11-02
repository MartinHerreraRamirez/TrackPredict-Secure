import { useCallback, useEffect, useState } from "react";

export type Tema = "claro" | "oscuro";
const CLAVE = "trackpredict-tema";

/** Tema guardado por la persona; si no hay, el del sistema. localStorage puede fallar (modo privado): se ignora. */
export function temaInicial(): Tema {
  try {
    const guardado = localStorage.getItem(CLAVE);
    if (guardado === "claro" || guardado === "oscuro") return guardado;
  } catch { /* sin almacenamiento: se usa el tema del sistema */ }
  return typeof matchMedia === "function" && matchMedia("(prefers-color-scheme: dark)").matches ? "oscuro" : "claro";
}

export function aplicarTema(t: Tema, guardar = true) {
  document.documentElement.dataset.tema = t;
  if (guardar) { try { localStorage.setItem(CLAVE, t); } catch { /* no pasa nada */ } }
  window.dispatchEvent(new CustomEvent("trackpredict-tema", { detail: t }));
}

export const otroTema = (t: Tema): Tema => (t === "claro" ? "oscuro" : "claro");

/** Tema actual y función para alternarlo. Todos los componentes que lo usan se actualizan a la vez. */
export function useTema(): [Tema, () => void] {
  const leer = () => (document.documentElement.dataset.tema === "oscuro" ? "oscuro" : "claro") as Tema;
  const [tema, setTema] = useState<Tema>(leer);
  useEffect(() => {
    const alCambiar = () => setTema(leer());
    window.addEventListener("trackpredict-tema", alCambiar);
    return () => window.removeEventListener("trackpredict-tema", alCambiar);
  }, []);
  const alternar = useCallback(() => aplicarTema(otroTema(leer())), []);
  return [tema, alternar];
}

/** Colores para los gráficos de Recharts (que no leen variables CSS de forma fiable). */
export function coloresGrafico(t: Tema) {
  return t === "oscuro"
    ? { grilla: "#2f4146", texto: "#9fb2b0", serie: "#5fc4b8", fondo: "#162226", borde: "#2f4146", tinta: "#e6efed" }
    : { grilla: "#d6dad5", texto: "#5d6b6e", serie: "#1f5f5b", fondo: "#ffffff", borde: "#d6dad5", tinta: "#1b2a2f" };
}
