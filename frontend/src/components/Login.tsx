import { FormEvent, useState } from "react";
import { api } from "../api";
import BotonTema from "./BotonTema";
import { errorDeRegistro } from "../validacion";
import type { Sesion } from "../types";

/** Cuentas ficticias de la semilla (db/init/02_seed.sql). Para ocultar los accesos rápidos al desplegar de verdad,
 *  construir el front con VITE_ACCESOS_RAPIDOS=false. */
const ACCESOS_RAPIDOS = import.meta.env.VITE_ACCESOS_RAPIDOS !== "false";
const CUENTAS = [
  { rol: "Administrador", email: "admin@trackpredict.com", password: "Admin123!", nota: "ve todo, reentrena modelos" },
  { rol: "Operador", email: "operador@trackpredict.com", password: "Operador123!", nota: "mapa y alertas" },
  { rol: "Auditor", email: "auditor@trackpredict.com", password: "Audit123!", nota: "solo lectura y bitácora" },
  { rol: "Otra empresa", email: "admin@norte.com", password: "Super123!", nota: "solo ve su vehículo" },
];

export default function Login({ onLogin, aviso }: { onLogin: (s: Sesion) => void; aviso?: string | null }) {
  const [modo, setModo] = useState<"ingreso" | "registro">("ingreso");
  const [email, setEmail] = useState(ACCESOS_RAPIDOS ? CUENTAS[0].email : "");
  const [password, setPassword] = useState(ACCESOS_RAPIDOS ? CUENTAS[0].password : "");
  const [nombre, setNombre] = useState("");
  const [empresa, setEmpresa] = useState("");
  const [repetir, setRepetir] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  function cambiarModo(m: "ingreso" | "registro") {
    setModo(m);
    setError(null);
    setPassword(m === "ingreso" && ACCESOS_RAPIDOS ? CUENTAS[0].password : "");
    setEmail(m === "ingreso" && ACCESOS_RAPIDOS ? CUENTAS[0].email : "");
    setRepetir("");
  }

  async function enviar(e?: FormEvent, credenciales = { email, password }) {
    e?.preventDefault();
    setCargando(true);
    setError(null);
    try {
      onLogin(await api.login(credenciales.email, credenciales.password));
    } catch {
      setError("Email o contraseña incorrectos. Revisá los datos e intentá de nuevo.");
    } finally {
      setCargando(false);
    }
  }

  async function registrar(e: FormEvent) {
    e.preventDefault();
    const problema = errorDeRegistro({ nombre, empresa, email, password, repetir });
    if (problema) { setError(problema); return; }
    setCargando(true);
    setError(null);
    try {
      onLogin(await api.registro({ nombre: nombre.trim(), empresa: empresa.trim(), email: email.trim(), password }));
    } catch (err) {
      setError(err instanceof Error ? err.message : "No se pudo crear la cuenta. Intentá de nuevo.");
    } finally {
      setCargando(false);
    }
  }

  if (modo === "registro") {
    return (
      <main className="login">
        <div className="tema-esquina"><BotonTema /></div>
        <form onSubmit={registrar} className="login-card">
          <h1>Crear cuenta</h1>
          <p className="muted">Se crea tu empresa y quedás como administrador. Después podés cargar tu flota.</p>
          <label>Tu nombre<input value={nombre} onChange={(e) => setNombre(e.target.value)} required autoFocus autoComplete="name" /></label>
          <label>Empresa<input value={empresa} onChange={(e) => setEmpresa(e.target.value)} required autoComplete="organization" /></label>
          <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" /></label>
          <label>Contraseña
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="new-password" />
            <span className="muted">Mínimo 8 caracteres, con letras y números.</span>
          </label>
          <label>Repetir contraseña<input type="password" value={repetir} onChange={(e) => setRepetir(e.target.value)} required autoComplete="new-password" /></label>
          {error && <p role="alert" className="error">{error}</p>}
          <button disabled={cargando}>{cargando ? "Creando cuenta…" : "Crear cuenta"}</button>
          <button type="button" className="ghost" onClick={() => cambiarModo("ingreso")} disabled={cargando}>Ya tengo cuenta</button>
        </form>
      </main>
    );
  }

  return (
    <main className="login">
      <div className="tema-esquina"><BotonTema /></div>
      <form onSubmit={enviar} className="login-card">
        <h1>TrackPredict</h1>
        <p className="muted">Centro de monitoreo de flotas</p>
        <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus /></label>
        <label>Contraseña<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
        {aviso && !error && <p role="status" className="banner">{aviso}</p>}
        {error && <p role="alert" className="error">{error}</p>}
        <button disabled={cargando}>{cargando ? "Ingresando…" : "Ingresar"}</button>
        <button type="button" className="ghost" onClick={() => cambiarModo("registro")} disabled={cargando}>Crear cuenta</button>
        {ACCESOS_RAPIDOS && (
          <div className="accesos-rapidos" aria-label="Accesos rápidos">
            <p className="muted">Accesos rápidos: tocá una cuenta para ingresar</p>
            {CUENTAS.map((c) => (
              <button type="button" className="ghost" key={c.email} disabled={cargando}
                onClick={() => { setEmail(c.email); setPassword(c.password); enviar(undefined, c); }}>
                <strong>{c.rol}</strong> · {c.email} / {c.password}
                <span className="muted"> — {c.nota}</span>
              </button>
            ))}
          </div>
        )}
      </form>
    </main>
  );
}
