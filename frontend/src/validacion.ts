/** Reglas de validación del formulario de registro. Espejan las del servidor (RegistroService), que es quien manda. */
export const PASSWORD_MIN = 8;

export function errorDePassword(p: string): string | null {
  if (p.length < PASSWORD_MIN) return `La contraseña debe tener al menos ${PASSWORD_MIN} caracteres.`;
  if (new TextEncoder().encode(p).length > 72) return "La contraseña es demasiado larga.";
  if (!/\p{L}/u.test(p) || !/\d/.test(p)) return "La contraseña debe combinar letras y números.";
  return null;
}

export interface DatosRegistro { nombre: string; empresa: string; email: string; password: string; repetir: string }

/** Devuelve el primer problema encontrado, o null si los datos son válidos. */
export function errorDeRegistro(d: DatosRegistro): string | null {
  if (d.nombre.trim().length < 2) return "Ingresá tu nombre.";
  if (d.empresa.trim().length < 2) return "Ingresá el nombre de tu empresa.";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.email.trim())) return "El email no parece válido.";
  const p = errorDePassword(d.password);
  if (p) return p;
  if (d.password !== d.repetir) return "Las contraseñas no coinciden.";
  return null;
}

/** Patente argentina: formato viejo (ABC123) o Mercosur (AB123CD). */
export function errorDePatente(p: string): string | null {
  return /^([A-Z]{3}\d{3}|[A-Z]{2}\d{3}[A-Z]{2})$/.test(p.trim().toUpperCase()) ? null : "La patente debe ser como ABC123 o AB123CD.";
}

export function errorDeConductor(nombre: string, documento: string): string | null {
  if (nombre.trim().length < 2) return "Ingresá el nombre del conductor.";
  if (!/^\d{6,10}$/.test(documento.trim())) return "El documento debe tener entre 6 y 10 números, sin puntos.";
  return null;
}

export function errorDeUsuarioNuevo(nombre: string, email: string, password: string): string | null {
  if (nombre.trim().length < 2) return "Ingresá el nombre.";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) return "El email no parece válido.";
  return errorDePassword(password);
}
