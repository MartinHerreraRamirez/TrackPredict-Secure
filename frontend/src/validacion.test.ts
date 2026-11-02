import { describe, expect, it } from "vitest";
import { errorDeConductor, errorDePassword, errorDePatente, errorDeRegistro, errorDeUsuarioNuevo } from "./validacion";

const ok = { nombre: "Ana Pérez", empresa: "Seguridad del Sur", email: "ana@ejemplo.com", password: "Clave2026ok", repetir: "Clave2026ok" };

describe("validación del registro", () => {
  it("acepta datos correctos", () => expect(errorDeRegistro(ok)).toBeNull());

  it("rechaza contraseñas débiles", () => {
    expect(errorDePassword("corta1")).not.toBeNull();
    expect(errorDePassword("sololetrasaqui")).not.toBeNull();
    expect(errorDePassword("123456789")).not.toBeNull();
    expect(errorDePassword("Clave2026ok")).toBeNull();
  });

  it("detecta email inválido, contraseñas distintas y campos vacíos", () => {
    expect(errorDeRegistro({ ...ok, email: "ana@" })).toMatch(/email/i);
    expect(errorDeRegistro({ ...ok, repetir: "otra2026clave" })).toMatch(/coinciden/);
    expect(errorDeRegistro({ ...ok, empresa: " " })).toMatch(/empresa/);
  });
});

describe("validación de la gestión", () => {
  it("acepta patentes viejas y Mercosur, con o sin minúsculas", () => {
    expect(errorDePatente("AB123CD")).toBeNull();
    expect(errorDePatente(" abc123 ")).toBeNull();
    expect(errorDePatente("A1234")).not.toBeNull();
  });
  it("exige documento numérico y nombre", () => {
    expect(errorDeConductor("Luis Gómez", "30123456")).toBeNull();
    expect(errorDeConductor("Luis Gómez", "30.123.456")).not.toBeNull();
    expect(errorDeConductor("L", "30123456")).not.toBeNull();
  });
  it("valida el alta de usuario", () => {
    expect(errorDeUsuarioNuevo("Ana", "ana@x.com", "Clave2026ok")).toBeNull();
    expect(errorDeUsuarioNuevo("Ana", "ana", "Clave2026ok")).not.toBeNull();
    expect(errorDeUsuarioNuevo("Ana", "ana@x.com", "corta")).not.toBeNull();
  });
});
