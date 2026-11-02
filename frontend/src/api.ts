import type { Alerta, Conductor, Usuario, AuditoriaItem, EstadoReentrenamiento, EtaResponse, EtaViaje, Geocerca, MetricasModelos, ResultadoReentrenamiento, Posicion, Riesgo, Ruta, Sesion, Vehiculo, Viaje } from "./types";

/** Cliente de la API. El front solo habla con el servicio Java; Python no se expone. */
export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

/** Quien quiera enterarse de que la sesion vencio (el token JWT dura 60 min) se registra aca; App vuelve al login. */
let alVencerSesion: (() => void) | null = null;
export function onSesionVencida(fn: (() => void) | null) { alVencerSesion = fn; }

async function request<T>(path: string, token?: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });
  if (!res.ok) {
    if (res.status === 401 && token) alVencerSesion?.();   // con token = sesion vencida; sin token = login fallido
    if (res.status === 401) throw new ApiError(401, "Sesión vencida o credenciales inválidas");
    const cuerpo = await res.json().catch(() => ({}));   // el backend envía { detail } o { message } controlados
    throw new ApiError(res.status, cuerpo.detail ?? cuerpo.message ?? `Error ${res.status}`);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export interface DatosVehiculo { patente: string; tipo: string; marca?: string; modelo?: string }
export interface DatosConductor { nombre: string; documento: string }

export const api = {
  login: (email: string, password: string) =>
    request<Sesion>("/api/auth/login", undefined, { method: "POST", body: JSON.stringify({ email, password }) }),
  registro: (d: { nombre: string; empresa: string; email: string; password: string }) =>
    request<Sesion>("/api/auth/registro", undefined, { method: "POST", body: JSON.stringify(d) }),
  vehiculos: (t: string) => request<Vehiculo[]>("/api/vehiculos", t),
  ultimasPosiciones: (t: string) => request<Posicion[]>("/api/posiciones/ultimas", t),
  alertas: (t: string) => request<Alerta[]>("/api/alertas", t),
  cambiarEstadoAlerta: (t: string, id: number, estado: string) =>
    request<Alerta>(`/api/alertas/${id}/estado`, t, { method: "PATCH", body: JSON.stringify({ estado }) }),
  eta: (t: string, body: { distanciaRestanteKm: number; velocidadMediaKmh: number; hora: number; diaSemana: number; minutosVentanaRestante?: number }) =>
    request<EtaResponse>("/api/predicciones/eta", t, { method: "POST", body: JSON.stringify(body) }),
  auditoria: (t: string) => request<AuditoriaItem[]>("/api/auditoria", t),
  rutas: (t: string) => request<Ruta[]>("/api/rutas", t),
  geocercas: (t: string) => request<Geocerca[]>("/api/geocercas", t),
  viajes: (t: string) => request<Viaje[]>("/api/viajes", t),
  planificarViaje: (t: string, body: { vehiculoId: number; rutaId: number }) =>
    request<Viaje>("/api/viajes", t, { method: "POST", body: JSON.stringify(body) }),
  accionViaje: (t: string, id: number, accion: "iniciar" | "finalizar" | "cancelar") =>
    request<Viaje>(`/api/viajes/${id}/${accion}`, t, { method: "POST" }),
  etaViaje: (t: string, id: number) => request<EtaViaje>(`/api/viajes/${id}/eta`, t),
  metricasModelos: (t: string) => request<MetricasModelos>("/api/modelos/metricas", t),
  estadoReentrenamiento: (t: string) => request<EstadoReentrenamiento>("/api/modelos/reentrenamiento", t),
  reentrenar: (t: string) => request<ResultadoReentrenamiento>("/api/modelos/reentrenar", t, { method: "POST" }),
  crearVehiculo: (t: string, d: DatosVehiculo) => request<Vehiculo>("/api/vehiculos", t, { method: "POST", body: JSON.stringify(d) }),
  actualizarVehiculo: (t: string, id: number, d: DatosVehiculo) => request<Vehiculo>(`/api/vehiculos/${id}`, t, { method: "PUT", body: JSON.stringify(d) }),
  borrarVehiculo: (t: string, id: number) => request<void>(`/api/vehiculos/${id}`, t, { method: "DELETE" }),
  conductores: (t: string) => request<Conductor[]>("/api/conductores", t),
  crearConductor: (t: string, d: DatosConductor) => request<Conductor>("/api/conductores", t, { method: "POST", body: JSON.stringify(d) }),
  actualizarConductor: (t: string, id: number, d: DatosConductor) => request<Conductor>(`/api/conductores/${id}`, t, { method: "PUT", body: JSON.stringify(d) }),
  borrarConductor: (t: string, id: number) => request<void>(`/api/conductores/${id}`, t, { method: "DELETE" }),
  usuarios: (t: string) => request<Usuario[]>("/api/usuarios", t),
  crearUsuario: (t: string, d: { nombre: string; email: string; password: string; rol: string }) =>
    request<Usuario>("/api/usuarios", t, { method: "POST", body: JSON.stringify(d) }),
  actualizarUsuario: (t: string, id: number, d: { nombre: string; rol: string; activo: boolean; passwordNueva?: string }) =>
    request<Usuario>(`/api/usuarios/${id}`, t, { method: "PUT", body: JSON.stringify(d) }),
  riesgoViaje: (t: string, id: number) => request<Riesgo>(`/api/viajes/${id}/riesgo`, t),
};
