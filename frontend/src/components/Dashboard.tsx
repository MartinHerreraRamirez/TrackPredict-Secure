import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { puedeGestionarAlertas, puedeVerMetricasModelos, puedeUsarPredictor, puedeVerAuditoria, puedeGestionarEmpresa, puedeVerMapa, puedeVerRutas } from "../permisos";
import type { Alerta, AuditoriaItem, Geocerca, Posicion, Rol, Ruta, Sesion, Vehiculo, Viaje } from "../types";
import BotonTema from "./BotonTema";
import Gestion from "./Gestion";
import MapaFlota, { PALETA } from "./MapaFlota";
import PanelEta from "./PanelEta";
import PanelModelos from "./PanelModelos";
import PanelViajes from "./PanelViajes";

const POLLING_MS = 4000;

type Vista = "monitoreo" | "viajes" | "prediccion" | "modelos" | "gestion" | "auditoria";

export default function Dashboard({ sesion, onSalir }: { sesion: Sesion; onSalir: () => void }) {
  const { token } = sesion;
  const esAdminReal = sesion.rol === "ADMIN";
  // "Ver como": el admin puede mirar la interfaz con los permisos de otro rol. Es solo visual: el servidor sigue autorizando como admin.
  const [rolVista, setRolVista] = useState<Rol | null>(null);
  const rol: Rol = esAdminReal && rolVista ? rolVista : sesion.rol;
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([]);
  const [posiciones, setPosiciones] = useState<Posicion[]>([]);
  const [alertas, setAlertas] = useState<Alerta[]>([]);
  const [auditoria, setAuditoria] = useState<AuditoriaItem[]>([]);
  const [viajes, setViajes] = useState<Viaje[]>([]);
  const [rutas, setRutas] = useState<Ruta[]>([]);
  const [geocercas, setGeocercas] = useState<Geocerca[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [vista, setVista] = useState<Vista>("monitoreo");
  const [seleccion, setSeleccion] = useState<number | null>(null);   // vehículo que se quiere mirar solo (null = toda la flota)

  const refrescar = useCallback(async () => {
    try {
      const [v, a, vj] = await Promise.all([api.vehiculos(token), api.alertas(token), api.viajes(token)]);
      setVehiculos(v);
      setAlertas(a);
      setViajes(vj);
      if (puedeVerMapa(rol)) setPosiciones(await api.ultimasPosiciones(token));
      if (puedeVerAuditoria(rol)) setAuditoria(await api.auditoria(token));
      setError(null);
    } catch {
      setError("No se pudo actualizar. Reintentando automáticamente…");
    }
  }, [token, rol]);

  // Rutas y geocercas cambian poco: se cargan al entrar y se vuelven a pedir cada 30 s, para que una ruta nueva aparezca sin recargar.
  useEffect(() => {
    if (!puedeVerRutas(rol)) return;
    const cargar = () => Promise.all([api.rutas(token), api.geocercas(token)]).then(([r, g]) => { setRutas(r); setGeocercas(g); }).catch(() => undefined);
    cargar();
    const id = setInterval(cargar, 30000);
    return () => clearInterval(id);
  }, [token, rol]);

  useEffect(() => {
    refrescar();
    const id = setInterval(refrescar, POLLING_MS);
    return () => clearInterval(id);
  }, [refrescar]);

  const patentes = useMemo(() => new Map(vehiculos.map((v) => [v.id, v.patente])), [vehiculos]);
  const recorridos = useMemo(() => viajes.filter((v) => v.estado === "EN_CURSO").map((v) => ({ vehiculoId: v.vehiculoId, rutaId: v.rutaId })), [viajes]);
  const colores = useMemo(() => new Map(vehiculos.map((v, i) => [v.id, PALETA[i % PALETA.length]])), [vehiculos]);
  const conAlerta = useMemo(() => new Set(alertas.filter((a) => a.estado !== "CERRADA").map((a) => a.vehiculoId)), [alertas]);

  // Con un vehículo elegido, el mapa, las alertas y los viajes se limitan a ese vehículo.
  const rutaDelVehiculo = useMemo(() => {
    if (seleccion === null) return null;
    const propios = viajes.filter((v) => v.vehiculoId === seleccion);
    return (propios.find((v) => v.estado === "EN_CURSO") ?? propios[0])?.rutaId ?? null;
  }, [seleccion, viajes]);
  const posicionesVista = seleccion === null ? posiciones : posiciones.filter((p) => p.vehiculoId === seleccion);
  const alertasVista = seleccion === null ? alertas : alertas.filter((a) => a.vehiculoId === seleccion);
  const viajesVista = seleccion === null ? viajes : viajes.filter((v) => v.vehiculoId === seleccion);

  const nuevas = alertas.filter((a) => a.estado === "NUEVA");
  const graves = new Set(["ANOMALIA_TRAYECTO", "SALIDA_GEOCERCA"]);   // las que más urgen: se destacan con parpadeo

  async function cambiarEstado(id: number, estado: string) {
    await api.cambiarEstadoAlerta(token, id, estado);
    refrescar();
  }

  const secciones: { id: Vista; titulo: string; visible: boolean; avisos?: number }[] = [
    { id: "monitoreo", titulo: "Monitoreo", visible: true, avisos: nuevas.length },
    { id: "viajes", titulo: "Viajes", visible: true },
    { id: "prediccion", titulo: "Predicción de ETA", visible: puedeUsarPredictor(rol) },
    { id: "modelos", titulo: "Modelos", visible: puedeVerMetricasModelos(rol) },
    { id: "gestion", titulo: "Gestión", visible: puedeGestionarEmpresa(rol) },
    { id: "auditoria", titulo: "Auditoría", visible: puedeVerAuditoria(rol) },
  ];
  const filtraPorVehiculo = vista === "monitoreo" || vista === "viajes";

  return (
    <div className="shell">
      <header className="topbar">
        <h1>TrackPredict</h1>
        <span className="muted">{sesion.nombre} · {sesion.rol.toLowerCase()}</span>
        {esAdminReal && (
          <label className="ver-como">Ver como
            <select value={rolVista ?? ""} onChange={(e) => { setRolVista((e.target.value || null) as Rol | null); setVista("monitoreo"); setSeleccion(null); }}>
              <option value="">Administrador (yo)</option>
              {(["OPERADOR", "SUPERVISOR", "AUDITOR"] as Rol[]).map((r) => <option key={r} value={r}>{r.charAt(0) + r.slice(1).toLowerCase()}</option>)}
            </select>
          </label>
        )}
        <BotonTema />
        <button className="ghost" onClick={onSalir}>Salir</button>
      </header>

      {esAdminReal && rolVista && (
        <p className="banner-ver-como" role="status">
          <span>Estás viendo la interfaz como <strong>{rolVista.toLowerCase()}</strong>, con los permisos de ese rol.</span>
          <button type="button" className="ghost" onClick={() => { setRolVista(null); setVista("monitoreo"); }}>Volver a administrador</button>
        </p>
      )}

      <nav className="navbar" aria-label="Secciones">
        <ul>
          {secciones.filter((x) => x.visible).map((x) => (
            <li key={x.id}>
              <button type="button" className={vista === x.id ? "activa" : undefined} aria-current={vista === x.id ? "page" : undefined}
                onClick={() => setVista(x.id)}>
                {x.titulo}
                {!!x.avisos && <span className="insignia" aria-label={`${x.avisos} alertas nuevas`}>{x.avisos}</span>}
              </button>
            </li>
          ))}
        </ul>
        {filtraPorVehiculo && (
          <label className="selector-vehiculo">Ver
            <select value={seleccion ?? ""} onChange={(e) => setSeleccion(e.target.value ? Number(e.target.value) : null)}>
              <option value="">Toda la flota ({vehiculos.length})</option>
              {vehiculos.map((v) => <option key={v.id} value={v.id}>{v.patente} · {v.tipo.toLowerCase()}</option>)}
            </select>
          </label>
        )}
      </nav>

      {error && <p role="status" className="banner">{error}</p>}
      {nuevas.length > 0 && (
        <p role="alert" className="alarma-banner">
          <span className="sirena" aria-hidden="true" />
          ALARMA: {nuevas.length === 1 ? "1 alerta nueva sin reconocer" : `${nuevas.length} alertas nuevas sin reconocer`}
          {" "}({[...new Set(nuevas.map((a) => patentes.get(a.vehiculoId) ?? `#${a.vehiculoId}`))].join(", ")})
        </p>
      )}

      {vista === "monitoreo" && (
        <>
          {puedeVerMapa(rol) && (
            <section className="panel mapa" aria-label="Mapa de la flota">
              <MapaFlota posiciones={posicionesVista} patentes={patentes} conAlerta={conAlerta} rutas={rutas} geocercas={geocercas}
                recorridos={recorridos} colores={colores} rutaSeleccionada={rutaDelVehiculo} vehiculoSeleccionado={seleccion} />
            </section>
          )}

          <section className="panel" aria-label="Alertas">
            <h2>Alertas</h2>
            {alertasVista.length === 0 ? (
              <p className="muted">Sin alertas. Cuando un vehículo incumpla una regla, aparecerá acá.</p>
            ) : (
              <ul className="alertas">
                {alertasVista.map((a) => (
                  <li key={a.id} data-estado={a.estado} className={a.estado === "NUEVA" && graves.has(a.tipo) ? "alerta-grave" : undefined}>
                    <div>
                      <strong>{patentes.get(a.vehiculoId) ?? `#${a.vehiculoId}`}</strong> {a.tipo.replaceAll("_", " ").toLowerCase()}
                      <div className="muted">{a.detalle}</div>
                    </div>
                    <span className="estado">{a.estado.replaceAll("_", " ").toLowerCase()}</span>
                    {puedeGestionarAlertas(rol) && a.estado !== "CERRADA" && (
                      <button className="ghost" onClick={() => cambiarEstado(a.id, a.estado === "NUEVA" ? "RECONOCIDA" : "CERRADA")}>
                        {a.estado === "NUEVA" ? "Reconocer" : "Cerrar"}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}

      {vista === "viajes" && (
        <PanelViajes token={token} rol={rol} viajes={viajesVista} vehiculos={vehiculos} rutas={rutas} onCambio={refrescar} />
      )}

      {/* Predicción y modelos quedan montados aunque estén ocultos: conservan el gráfico calculado al cambiar de sección. */}
      {puedeUsarPredictor(rol) && <div hidden={vista !== "prediccion"}><PanelEta token={token} /></div>}
      {puedeVerMetricasModelos(rol) && <div hidden={vista !== "modelos"}><PanelModelos token={token} rol={rol} /></div>}

      {vista === "gestion" && puedeGestionarEmpresa(rol) && <Gestion token={token} miId={sesion.usuarioId} onCambio={refrescar} />}

      {vista === "auditoria" && puedeVerAuditoria(rol) && (
        <section className="panel" aria-label="Auditoría">
          <h2>Bitácora de auditoría</h2>
          <table>
            <thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Recurso</th></tr></thead>
            <tbody>
              {auditoria.map((x) => (
                <tr key={x.id}><td>{new Date(x.ts).toLocaleString("es-AR")}</td><td>{x.usuario_id}</td><td>{x.accion}</td><td>{x.recurso}</td></tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  );
}
