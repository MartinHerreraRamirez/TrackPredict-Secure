import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api, ApiError } from "../api";
import type { Conductor, Rol, Usuario, Vehiculo } from "../types";
import { errorDeConductor, errorDePassword, errorDePatente, errorDeUsuarioNuevo } from "../validacion";

type Seccion = "vehiculos" | "conductores" | "usuarios";
const TIPOS = ["BLINDADO", "AUTO", "ESCOLTA"];
const ROLES: Rol[] = ["ADMIN", "OPERADOR", "SUPERVISOR", "AUDITOR"];
const DESCRIPCION_ROL: Record<Rol, string> = {
  ADMIN: "Todo, incluida la gestión de la empresa",
  OPERADOR: "Monitorea el mapa y atiende alertas",
  SUPERVISOR: "Además planifica viajes y ve conductores",
  AUDITOR: "Solo consulta la bitácora",
};

/** ABM de la empresa: vehículos, conductores y usuarios. Solo para ADMIN; el servidor vuelve a validar todo. */
export default function Gestion({ token, miId, onCambio }: { token: string; miId?: number; onCambio: () => void }) {
  const [seccion, setSeccion] = useState<Seccion>("vehiculos");
  const [aviso, setAviso] = useState<{ tipo: "ok" | "error"; texto: string } | null>(null);
  const ok = (texto: string) => setAviso({ tipo: "ok", texto });
  const falla = (e: unknown) => setAviso({ tipo: "error", texto: e instanceof ApiError ? e.message : "No se pudo completar la operación." });

  return (
    <section className="gestion" aria-label="Gestión">
      <div className="subtabs" role="tablist">
        {(["vehiculos", "conductores", "usuarios"] as Seccion[]).map((s) => (
          <button key={s} type="button" role="tab" aria-selected={seccion === s} className={seccion === s ? "activa" : undefined}
            onClick={() => { setSeccion(s); setAviso(null); }}>
            {s === "vehiculos" ? "Vehículos" : s === "conductores" ? "Conductores" : "Usuarios"}
          </button>
        ))}
      </div>
      {aviso && <p className={aviso.tipo === "ok" ? "aviso-ok" : "error"} role={aviso.tipo === "ok" ? "status" : "alert"}>{aviso.texto}</p>}
      {seccion === "vehiculos" && <GestionVehiculos token={token} ok={ok} falla={falla} onCambio={onCambio} />}
      {seccion === "conductores" && <GestionConductores token={token} ok={ok} falla={falla} />}
      {seccion === "usuarios" && <GestionUsuarios token={token} miId={miId} ok={ok} falla={falla} />}
    </section>
  );
}

interface Props { token: string; ok: (t: string) => void; falla: (e: unknown) => void }

function GestionVehiculos({ token, ok, falla, onCambio }: Props & { onCambio: () => void }) {
  const [lista, setLista] = useState<Vehiculo[]>([]);
  const [editando, setEditando] = useState<number | null>(null);
  const [f, setF] = useState({ patente: "", tipo: "AUTO", marca: "", modelo: "" });
  const [errorForm, setErrorForm] = useState<string | null>(null);
  const cargar = useCallback(() => api.vehiculos(token).then(setLista).catch(falla), [token, falla]);
  useEffect(() => { cargar(); }, [cargar]);

  const limpiar = () => { setEditando(null); setF({ patente: "", tipo: "AUTO", marca: "", modelo: "" }); setErrorForm(null); };

  async function guardar(e: FormEvent) {
    e.preventDefault();
    const m = errorDePatente(f.patente);
    if (m) return setErrorForm(m);
    setErrorForm(null);
    try {
      const datos = { patente: f.patente.trim().toUpperCase(), tipo: f.tipo, marca: f.marca.trim(), modelo: f.modelo.trim() };
      if (editando === null) await api.crearVehiculo(token, datos); else await api.actualizarVehiculo(token, editando, datos);
      ok(editando === null ? "Vehículo agregado." : "Vehículo actualizado.");
      limpiar(); await cargar(); onCambio();
    } catch (err) { falla(err); }
  }

  async function borrar(v: Vehiculo) {
    if (!window.confirm(`¿Dar de baja el vehículo ${v.patente}? Se conserva su historial de viajes.`)) return;
    try { await api.borrarVehiculo(token, v.id); ok(`Vehículo ${v.patente} dado de baja.`); await cargar(); onCambio(); } catch (err) { falla(err); }
  }

  return (
    <div className="gestion-grid">
      <form onSubmit={guardar} className="form-gestion" noValidate>
        <h3>{editando === null ? "Agregar vehículo" : "Editar vehículo"}</h3>
        <label>Patente<input value={f.patente} onChange={(e) => setF({ ...f, patente: e.target.value })} placeholder="AB123CD" maxLength={12} required /></label>
        <label>Tipo<select value={f.tipo} onChange={(e) => setF({ ...f, tipo: e.target.value })}>{TIPOS.map((t) => <option key={t}>{t}</option>)}</select></label>
        <label>Marca<input value={f.marca} onChange={(e) => setF({ ...f, marca: e.target.value })} maxLength={60} /></label>
        <label>Modelo<input value={f.modelo} onChange={(e) => setF({ ...f, modelo: e.target.value })} maxLength={60} /></label>
        {errorForm && <p className="error" role="alert">{errorForm}</p>}
        <div className="fila"><button type="submit">{editando === null ? "Agregar" : "Guardar"}</button>
          {editando !== null && <button type="button" className="ghost" onClick={limpiar}>Cancelar</button>}</div>
        <p className="muted nota">Un vehículo nuevo aparece en el mapa cuando su dispositivo GPS empieza a enviar posiciones.</p>
      </form>
      <Tabla vacio="No hay vehículos cargados." cols={["Patente", "Tipo", "Marca / modelo", ""]}
        filas={lista.map((v) => [v.patente, v.tipo, [v.marca, v.modelo].filter(Boolean).join(" ") || "—",
          <Acciones key={v.id} onEditar={() => { setEditando(v.id); setF({ patente: v.patente, tipo: v.tipo, marca: v.marca ?? "", modelo: v.modelo ?? "" }); setErrorForm(null); }} onBorrar={() => borrar(v)} />])} />
    </div>
  );
}

function GestionConductores({ token, ok, falla }: Props) {
  const [lista, setLista] = useState<Conductor[]>([]);
  const [editando, setEditando] = useState<number | null>(null);
  const [f, setF] = useState({ nombre: "", documento: "" });
  const [errorForm, setErrorForm] = useState<string | null>(null);
  const cargar = useCallback(() => api.conductores(token).then(setLista).catch(falla), [token, falla]);
  useEffect(() => { cargar(); }, [cargar]);
  const limpiar = () => { setEditando(null); setF({ nombre: "", documento: "" }); setErrorForm(null); };

  async function guardar(e: FormEvent) {
    e.preventDefault();
    const m = errorDeConductor(f.nombre, f.documento);
    if (m) return setErrorForm(m);
    setErrorForm(null);
    try {
      const datos = { nombre: f.nombre.trim(), documento: f.documento.trim() };
      if (editando === null) await api.crearConductor(token, datos); else await api.actualizarConductor(token, editando, datos);
      ok(editando === null ? "Conductor agregado." : "Conductor actualizado.");
      limpiar(); await cargar();
    } catch (err) { falla(err); }
  }

  async function borrar(c: Conductor) {
    if (!window.confirm(`¿Dar de baja a ${c.nombre}?`)) return;
    try { await api.borrarConductor(token, c.id); ok(`${c.nombre} dado de baja.`); await cargar(); } catch (err) { falla(err); }
  }

  return (
    <div className="gestion-grid">
      <form onSubmit={guardar} className="form-gestion" noValidate>
        <h3>{editando === null ? "Agregar conductor" : "Editar conductor"}</h3>
        <label>Nombre y apellido<input value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} maxLength={120} required /></label>
        <label>Documento (DNI)<input value={f.documento} onChange={(e) => setF({ ...f, documento: e.target.value })} inputMode="numeric" placeholder="30123456" maxLength={10} required /></label>
        {errorForm && <p className="error" role="alert">{errorForm}</p>}
        <div className="fila"><button type="submit">{editando === null ? "Agregar" : "Guardar"}</button>
          {editando !== null && <button type="button" className="ghost" onClick={limpiar}>Cancelar</button>}</div>
      </form>
      <Tabla vacio="No hay conductores cargados." cols={["Nombre", "Documento", ""]}
        filas={lista.map((c) => [c.nombre, c.documento,
          <Acciones key={c.id} onEditar={() => { setEditando(c.id); setF({ nombre: c.nombre, documento: c.documento }); setErrorForm(null); }} onBorrar={() => borrar(c)} />])} />
    </div>
  );
}

function GestionUsuarios({ token, miId, ok, falla }: Props & { miId?: number }) {
  const [lista, setLista] = useState<Usuario[]>([]);
  const [editando, setEditando] = useState<Usuario | null>(null);
  const [f, setF] = useState({ nombre: "", email: "", password: "", rol: "OPERADOR" as Rol, activo: true });
  const [errorForm, setErrorForm] = useState<string | null>(null);
  const cargar = useCallback(() => api.usuarios(token).then(setLista).catch(falla), [token, falla]);
  useEffect(() => { cargar(); }, [cargar]);
  const limpiar = () => { setEditando(null); setF({ nombre: "", email: "", password: "", rol: "OPERADOR", activo: true }); setErrorForm(null); };
  const soyYo = editando !== null && editando.id === miId;

  async function guardar(e: FormEvent) {
    e.preventDefault();
    try {
      if (editando === null) {
        const m = errorDeUsuarioNuevo(f.nombre, f.email, f.password);
        if (m) return setErrorForm(m);
        await api.crearUsuario(token, { nombre: f.nombre.trim(), email: f.email.trim(), password: f.password, rol: f.rol });
        ok("Usuario creado. Ya puede ingresar con ese email y contraseña.");
      } else {
        if (f.nombre.trim().length < 2) return setErrorForm("Ingresá el nombre.");
        if (f.password) { const m = errorDePassword(f.password); if (m) return setErrorForm(m); }
        await api.actualizarUsuario(token, editando.id, { nombre: f.nombre.trim(), rol: f.rol, activo: f.activo, passwordNueva: f.password || undefined });
        ok("Usuario actualizado.");
      }
      limpiar(); await cargar();
    } catch (err) { falla(err); }
  }

  return (
    <div className="gestion-grid">
      <form onSubmit={guardar} className="form-gestion" noValidate>
        <h3>{editando === null ? "Agregar usuario" : `Editar ${editando.email}`}</h3>
        <label>Nombre<input value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} maxLength={120} required /></label>
        {editando === null && <label>Email<input type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} maxLength={160} required /></label>}
        <label>{editando === null ? "Contraseña" : "Contraseña nueva (opcional)"}
          <input type="password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} autoComplete="new-password" maxLength={72} /></label>
        <label>Rol<select value={f.rol} disabled={soyYo} onChange={(e) => setF({ ...f, rol: e.target.value as Rol })}>{ROLES.map((r) => <option key={r} value={r}>{r.toLowerCase()}</option>)}</select></label>
        <p className="muted nota">{DESCRIPCION_ROL[f.rol]}</p>
        {editando !== null && (
          <label className="check"><input type="checkbox" checked={f.activo} disabled={soyYo} onChange={(e) => setF({ ...f, activo: e.target.checked })} /> Cuenta activa</label>
        )}
        {soyYo && <p className="muted nota">No podés cambiar tu propio rol ni darte de baja.</p>}
        {errorForm && <p className="error" role="alert">{errorForm}</p>}
        <div className="fila"><button type="submit">{editando === null ? "Crear usuario" : "Guardar"}</button>
          {editando !== null && <button type="button" className="ghost" onClick={limpiar}>Cancelar</button>}</div>
      </form>
      <Tabla vacio="No hay usuarios." cols={["Nombre", "Email", "Rol", "Estado", ""]}
        filas={lista.map((u) => [u.nombre, u.email, u.rol.toLowerCase(), u.activo ? "Activo" : "Baja",
          <Acciones key={u.id} onEditar={() => { setEditando(u); setF({ nombre: u.nombre, email: u.email, password: "", rol: u.rol, activo: u.activo }); setErrorForm(null); }} />])} />
    </div>
  );
}

function Acciones({ onEditar, onBorrar }: { onEditar: () => void; onBorrar?: () => void }) {
  return (
    <span className="acciones">
      <button type="button" className="ghost" onClick={onEditar}>Editar</button>
      {onBorrar && <button type="button" className="ghost peligro" onClick={onBorrar}>Dar de baja</button>}
    </span>
  );
}

function Tabla({ cols, filas, vacio }: { cols: string[]; filas: React.ReactNode[][]; vacio: string }) {
  if (filas.length === 0) return <p className="muted">{vacio}</p>;
  return (
    <div className="tabla-gestion">
      <table>
        <thead><tr>{cols.map((c, i) => <th key={i} scope="col">{c}</th>)}</tr></thead>
        <tbody>{filas.map((fila, i) => <tr key={i}>{fila.map((celda, j) => <td key={j}>{celda}</td>)}</tr>)}</tbody>
      </table>
    </div>
  );
}
