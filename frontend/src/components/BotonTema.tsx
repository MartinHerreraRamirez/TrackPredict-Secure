import { useTema } from "../tema";

/** Alterna entre tema claro y oscuro. El botón anuncia a qué tema se pasará al tocarlo. */
export default function BotonTema() {
  const [tema, alternar] = useTema();
  const aOscuro = tema === "claro";
  return (
    <button type="button" className="ghost boton-tema" onClick={alternar} aria-label={aOscuro ? "Cambiar a tema oscuro" : "Cambiar a tema claro"}
      title={aOscuro ? "Cambiar a tema oscuro" : "Cambiar a tema claro"}>
      <span aria-hidden="true">{aOscuro ? "☾" : "☀"}</span> {aOscuro ? "Tema oscuro" : "Tema claro"}
    </button>
  );
}
