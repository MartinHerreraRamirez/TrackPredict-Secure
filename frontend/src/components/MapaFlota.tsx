import L from "leaflet";
import { useEffect } from "react";
import { Circle, MapContainer, Marker, Polygon, Polyline, Popup, TileLayer, Tooltip, useMap } from "react-leaflet";
import type { Geocerca, Posicion, Ruta } from "../types";

const CENTRO: [number, number] = [-34.609, -58.375];

/** Un color por vehículo: su marcador y la ruta de su viaje en curso comparten color, así se ve si se salió de su recorrido. */
export const PALETA = ["#1f5f5b", "#2b6cb0", "#b7791f", "#6b46c1", "#2f855a", "#c05621", "#b83280", "#4a5568"];
const GRIS_RUTA = "#8a9a98";

function icono(alerta: boolean, color: string) {
  // Con alerta, el punto late en rojo (CSS .marcador-alerta); sin alerta, lleva el color del vehículo.
  return L.divIcon({
    className: "",
    html: `<span class="marcador ${alerta ? "marcador-alerta" : "marcador-ok"}" style="${alerta ? "" : `background:${color};box-shadow:0 0 0 1px ${color}`}"></span>`,
    iconSize: [18, 18],
    iconAnchor: [9, 9],
  });
}

const aLatLng = (pts: { lat: number; lon: number }[]): [number, number][] => pts.map((p) => [p.lat, p.lon]);

/** Encuadra el mapa sobre las rutas visibles y los vehículos mostrados (así un vehículo fuera de su ruta no queda
 *  fuera de pantalla). Se repite solo cuando cambia el conjunto mostrado (rutas, vehículo elegido, llegada de la
 *  primera posición), no con cada actualización; y a pedido con el botón "Encuadrar". */
function puntosAEncuadrar(rutas: Ruta[], posiciones: Posicion[]): [number, number][] {
  return [...rutas.flatMap((r) => aLatLng(r.puntos)), ...posiciones.map((p): [number, number] => [p.lat, p.lon])];
}

function AjustarVista({ rutas, posiciones, clave }: { rutas: Ruta[]; posiciones: Posicion[]; clave: string }) {
  const map = useMap();
  useEffect(() => {
    const pts = puntosAEncuadrar(rutas, posiciones);
    if (pts.length > 0) map.fitBounds(L.latLngBounds(pts), { padding: [40, 40], maxZoom: 16 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave, map]);
  return null;
}

function BotonEncuadrar({ rutas, posiciones }: { rutas: Ruta[]; posiciones: Posicion[] }) {
  const map = useMap();
  return (
    <div className="leaflet-bottom leaflet-left">
      <div className="leaflet-control leaflet-bar">
        <button type="button" className="boton-encuadrar" title="Ajustar el mapa a las rutas y vehículos"
          onClick={() => { const pts = puntosAEncuadrar(rutas, posiciones); if (pts.length) map.fitBounds(L.latLngBounds(pts), { padding: [40, 40], maxZoom: 16 }); }}>
          Encuadrar
        </button>
      </div>
    </div>
  );
}

interface Props {
  posiciones: Posicion[];
  patentes: Map<number, string>;
  conAlerta: Set<number>;
  rutas: Ruta[];
  geocercas: Geocerca[];
  /** Viajes en curso (vehículo y ruta): cada ruta se pinta con el color del vehículo que la recorre; el resto, en gris. */
  recorridos: { vehiculoId: number; rutaId: number }[];
  /** Color de cada vehículo. */
  colores: Map<number, string>;
  /** Con un vehículo elegido: id de su ruta (o null si todavía no tiene viaje) y el propio vehículo. */
  rutaSeleccionada?: number | null;
  vehiculoSeleccionado?: number | null;
}

export default function MapaFlota({ posiciones, patentes, conAlerta, rutas, geocercas, recorridos, colores, rutaSeleccionada = null, vehiculoSeleccionado = null }: Props) {
  // Con un vehículo elegido que tiene ruta, solo se dibuja esa ruta y su corredor; si no tiene, se ve toda la flota.
  const filtrar = vehiculoSeleccionado !== null && rutaSeleccionada !== null;
  const rutasVista = filtrar ? rutas.filter((r) => r.id === rutaSeleccionada) : rutas;
  const geocercasVista = filtrar ? geocercas.filter((g) => g.rutaId === rutaSeleccionada) : geocercas;
  const colorDe = (id: number) => colores.get(id) ?? PALETA[0];
  const duenos = (rutaId: number) => recorridos.filter((r) => r.rutaId === rutaId).map((r) => r.vehiculoId);
  const leyenda = recorridos.filter((r) => vehiculoSeleccionado === null || r.vehiculoId === vehiculoSeleccionado);
  return (
    <>
    <MapContainer center={CENTRO} zoom={13} style={{ height: "100%", width: "100%" }}>
      <AjustarVista rutas={rutasVista} posiciones={posiciones} clave={`${rutasVista.map((r) => r.id).join(",")}|${vehiculoSeleccionado ?? "todos"}|${posiciones.length > 0}`} />
      <BotonEncuadrar rutas={rutasVista} posiciones={posiciones} />
      <TileLayer attribution="&copy; OpenStreetMap" url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" />
      {geocercasVista.map((g) => (
        <Polygon key={`g${g.id}`} positions={aLatLng(g.puntos)}
          pathOptions={{ color: "#b7791f", weight: 1, dashArray: "5 5", fillOpacity: 0.07 }}>
          <Popup>{g.nombre}</Popup>
        </Polygon>
      ))}
      {rutasVista.map((r) => {
        const d = duenos(r.id);
        const activa = d.length > 0;
        return (
          <Polyline key={`r${r.id}`} positions={aLatLng(r.puntos)}
            pathOptions={{ color: activa ? colorDe(d[0]) : GRIS_RUTA, weight: activa ? 5 : 3, opacity: activa ? 0.9 : 0.45 }}>
            <Popup>
              <strong>{r.nombre}</strong><br />{r.duracionPlanMin} min · máx. {r.velocidadMaxKmh} km/h
              {activa && <><br />Recorrido de {d.map((id) => patentes.get(id) ?? `#${id}`).join(", ")}</>}
            </Popup>
          </Polyline>
        );
      })}
      {posiciones.filter((p) => conAlerta.has(p.vehiculoId)).map((p) => (
        // Halo rojo que late alrededor del vehículo con alerta (la animación está en styles.css)
        <Circle key={`h${p.vehiculoId}`} center={[p.lat, p.lon]} radius={350}
          pathOptions={{ color: "#e5322d", weight: 2, fillColor: "#e5322d", className: "halo-alerta" }} interactive={false} />
      ))}
      {posiciones.map((p) => (
        <Marker key={p.vehiculoId} position={[p.lat, p.lon]} icon={icono(conAlerta.has(p.vehiculoId), colorDe(p.vehiculoId))}>
          <Tooltip permanent direction="right" offset={[10, 0]}>{patentes.get(p.vehiculoId) ?? `#${p.vehiculoId}`}</Tooltip>
          <Popup>
            <strong>{patentes.get(p.vehiculoId) ?? `#${p.vehiculoId}`}</strong><br />
            {p.velocidadKmh.toFixed(0)} km/h · {new Date(p.ts).toLocaleTimeString("es-AR")}
          </Popup>
        </Marker>
      ))}
    </MapContainer>
    {leyenda.length > 0 && (
      <ul className="leyenda-mapa" aria-label="Recorrido de cada vehículo">
        {leyenda.map((r) => (
          <li key={r.vehiculoId}>
            <span className="muestra" style={{ background: colorDe(r.vehiculoId) }} />
            <strong>{patentes.get(r.vehiculoId) ?? `#${r.vehiculoId}`}</strong> {rutas.find((x) => x.id === r.rutaId)?.nombre ?? `ruta ${r.rutaId}`}
          </li>
        ))}
      </ul>
    )}
    </>
  );
}
