import { describe, expect, it } from "vitest";
import { puedeReentrenarModelos, puedeGestionarAlertas, puedeGestionarViajes, puedeVerAuditoria, puedeVerMapa } from "./permisos";

describe("permisos por rol", () => {
  it("el auditor solo lee: no ve el mapa ni gestiona alertas, pero ve la bitácora", () => {
    expect(puedeVerMapa("AUDITOR")).toBe(false);
    expect(puedeGestionarAlertas("AUDITOR")).toBe(false);
    expect(puedeVerAuditoria("AUDITOR")).toBe(true);
  });
  it("el operador atiende alertas pero no ve la bitácora", () => {
    expect(puedeGestionarAlertas("OPERADOR")).toBe(true);
    expect(puedeVerAuditoria("OPERADOR")).toBe(false);
  });
  it("solo administrador y supervisor planifican e inician viajes", () => {
    expect(puedeGestionarViajes("ADMIN")).toBe(true);
    expect(puedeGestionarViajes("SUPERVISOR")).toBe(true);
    expect(puedeGestionarViajes("OPERADOR")).toBe(false);
    expect(puedeGestionarViajes("AUDITOR")).toBe(false);
  });
  it("solo el administrador reentrena los modelos", () => {
    expect(puedeReentrenarModelos("ADMIN")).toBe(true);
    expect(puedeReentrenarModelos("SUPERVISOR")).toBe(false);
    expect(puedeReentrenarModelos("OPERADOR")).toBe(false);
    expect(puedeReentrenarModelos("AUDITOR")).toBe(false);
  });
});
