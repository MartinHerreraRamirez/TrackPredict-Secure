import { useEffect, useState } from "react";
import { onSesionVencida } from "./api";
import Login from "./components/Login";
import Dashboard from "./components/Dashboard";
import type { Sesion } from "./types";

export default function App() {
  const [sesion, setSesion] = useState<Sesion | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  // Si el servidor responde 401 con una sesion abierta, el token venció: volver al login con un aviso claro.
  useEffect(() => {
    onSesionVencida(() => { setSesion(null); setAviso("Tu sesión venció. Ingresá de nuevo."); });
    return () => onSesionVencida(null);
  }, []);

  return sesion
    ? <Dashboard sesion={sesion} onSalir={() => { setSesion(null); setAviso(null); }} />
    : <Login aviso={aviso} onLogin={(s) => { setAviso(null); setSesion(s); }} />;
}
