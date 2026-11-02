export type Rol = "ADMIN" | "OPERADOR" | "SUPERVISOR" | "AUDITOR";

export interface Sesion { token: string; rol: Rol; nombre: string; empresaId: number; usuarioId?: number }
export interface Vehiculo { id: number; patente: string; tipo: string; marca?: string; modelo?: string }
export interface Posicion { id: number; vehiculoId: number; ts: string; lat: number; lon: number; velocidadKmh: number }
export interface Alerta { id: number; vehiculoId: number; tipo: string; detalle: string; estado: string; creadaEn: string }
export interface EtaResponse { eta_minutos: number; intervalo_minutos: [number, number]; prob_cumple_ventana: number | null }
export interface AuditoriaItem { id: number; usuario_id: number; accion: string; recurso: string; ts: string }

export interface Punto { lat: number; lon: number }
export interface Ruta { id: number; nombre: string; duracionPlanMin: number; velocidadMaxKmh: number; puntos: Punto[] }
export interface Geocerca { id: number; rutaId: number; nombre: string; puntos: Punto[] }
export interface Viaje {
  id: number; vehiculoId: number; conductorId: number | null; rutaId: number; estado: string;
  inicioPlan: string | null; finPlan: string | null; inicioReal: string | null; finReal: string | null;
}
export interface EtaViaje extends EtaResponse { distancia_restante_km: number; velocidad_media_kmh: number }
export interface Riesgo { puntaje_riesgo: number; nivel: "NORMAL" | "ALERTA" | "CRITICO"; factores: Record<string, number> }

export interface MetricasEta {
  mae_min: number; rmse_min: number; r2: number; mae_baseline_min: number;
  muestras_entrenamiento: number; observaciones_reales: number;
}
export interface MetricasAnomalias {
  falsos_positivos_alerta: number; deteccion_anomalias_alerta: number; deteccion_anomalias_critico: number;
}
export interface MetricasModelos { eta: MetricasEta; anomalias: MetricasAnomalias }
export interface EstadoReentrenamiento { viajesDisponibles: number; minimoRequerido: number }
export interface ResultadoReentrenamiento { mensaje: string; metricas: MetricasEta }

export interface Conductor { id: number; nombre: string; documento: string }
export interface Usuario { id: number; email: string; nombre: string; rol: Rol; activo: boolean }
