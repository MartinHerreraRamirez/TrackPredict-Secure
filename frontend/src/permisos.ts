import type { Rol } from "./types";

/** Espejo de los @PreAuthorize del backend (la autoridad real siempre es el servidor). */
export const puedeVerMapa = (r: Rol) => r !== "AUDITOR";
export const puedeGestionarAlertas = (r: Rol) => r !== "AUDITOR";
export const puedeVerAuditoria = (r: Rol) => r === "ADMIN" || r === "AUDITOR";
export const puedeUsarPredictor = (r: Rol) => r !== "AUDITOR";
export const puedeGestionarViajes = (r: Rol) => r === "ADMIN" || r === "SUPERVISOR";
export const puedeVerRutas = (r: Rol) => r !== "AUDITOR";
export const puedeVerMetricasModelos = (_r: Rol) => true;   // los modelos son globales y no contienen datos de empresas
export const puedeReentrenarModelos = (r: Rol) => r === "ADMIN";
export const puedeGestionarEmpresa = (r: Rol) => r === "ADMIN";   // altas, bajas y cambios de vehículos, conductores y usuarios
