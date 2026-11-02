"""Simulador GPS de TrackPredict. Genera viajes sinteticos (nunca datos reales) sobre una ruta de Buenos Aires
y los envia a la API de ingesta del servicio Java autenticandose como dispositivo (X-Device-Key).

Uso:  python simulator.py --api http://localhost:8080 --key demo-device-key-001 --anomalia
"""
import argparse
import math
import random
import threading
import time
from datetime import datetime, timedelta, timezone

import httpx

# Misma ruta que la semilla de la base (lat, lon): Microcentro -> Puerto Madero
RUTA = [(-34.6037, -58.3816), (-34.6080, -58.3700), (-34.6130, -58.3640), (-34.6200, -58.3620)]
R = 6371000.0

# Rutas de la semilla de la base (db/init/02_seed.sql), como (lat, lon). Mismo nombre que en la base.
RUTAS = {
    "Microcentro - Puerto Madero": RUTA,
    "Retiro - Palermo": [(-34.5918, -58.3737), (-34.5885, -58.3830), (-34.5850, -58.3925), (-34.5820, -58.4010)],
    "Constitucion - La Boca": [(-34.6277, -58.3815), (-34.6320, -58.3770), (-34.6365, -58.3705), (-34.6395, -58.3640)],
    # Trayectos largos (esquematicos: siguen de forma aproximada las autopistas, no el trazado exacto)
    "Microcentro - Aeropuerto Ezeiza": [(-34.6037, -58.3816), (-34.6150, -58.4000), (-34.6400, -58.4500), (-34.6600, -58.5000),
                                        (-34.7000, -58.5200), (-34.7500, -58.5300), (-34.8000, -58.5250), (-34.8222, -58.5358)],
    "Retiro - Tigre": [(-34.5918, -58.3737), (-34.5700, -58.4100), (-34.5480, -58.4450), (-34.5200, -58.4750),
                       (-34.4900, -58.5100), (-34.4600, -58.5400), (-34.4260, -58.5790)],
    "Puerto Madero - La Plata": [(-34.6200, -58.3620), (-34.6350, -58.3600), (-34.6700, -58.3300), (-34.7100, -58.2700),
                                 (-34.7600, -58.2100), (-34.8100, -58.1500), (-34.8600, -58.0600), (-34.9000, -57.9900),
                                 (-34.9205, -57.9536)],
    "Microcentro - San Isidro": [(-34.6037, -58.3816), (-34.5850, -58.4050), (-34.5600, -58.4350), (-34.5300, -58.4650),
                                 (-34.5000, -58.4900), (-34.4730, -58.5110)],
}

# Velocidad media de cada ruta en el simulador (km/h): urbana lenta, autopista rapida. Se mantiene bajo el limite de
# 80 km/h del backend (TOPE_KMH) para que la demo no genere alertas de exceso de velocidad sin querer.
TOPE_KMH = 78.0
VEL_RUTA = {
    "Microcentro - Puerto Madero": 34.0, "Retiro - Palermo": 38.0, "Constitucion - La Boca": 30.0,
    "Microcentro - Aeropuerto Ezeiza": 60.0, "Retiro - Tigre": 52.0, "Puerto Madero - La Plata": 65.0,
    "Microcentro - San Isidro": 48.0,
}

# Flota de demostracion: un dispositivo por vehiculo de Seguridad Demo (claves de la semilla).
# Cada vehiculo recorre su circuito en bucle: empieza por el trayecto largo y despues alterna con los cortos.
FLOTA = [
    {"patente": "AB123CD", "key": "demo-device-key-001", "rutas": ["Microcentro - Aeropuerto Ezeiza", "Microcentro - Puerto Madero"]},
    {"patente": "AC456EF", "key": "demo-device-key-002", "rutas": ["Retiro - Tigre", "Retiro - Palermo", "Microcentro - San Isidro"]},
    {"patente": "AD789GH", "key": "demo-device-key-003", "rutas": ["Puerto Madero - La Plata", "Constitucion - La Boca"]},
]


def haversine_m(a, b) -> float:
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dphi, dl = p2 - p1, math.radians(b[1] - a[1])
    h = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * R * math.asin(math.sqrt(h))


def punto_en_ruta(ruta, dist_m):
    """Punto (lat, lon) a dist_m metros del inicio, recorriendo la polilinea."""
    restante = dist_m
    for a, b in zip(ruta, ruta[1:]):
        seg = haversine_m(a, b)
        if restante <= seg:
            f = restante / seg
            return a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f
        restante -= seg
    return ruta[-1]


def largo_ruta(ruta) -> float:
    return sum(haversine_m(a, b) for a, b in zip(ruta, ruta[1:]))


def generar_viaje(ruta=RUTA, vel_media_kmh=34.0, intervalo_s=5, anomalia=False, seed=None, t0=None):
    """Devuelve la lista de posiciones de un viaje completo.
    anomalia=True: a mitad de viaje el vehiculo se desvia ~700 m de la ruta y luego queda detenido un rato."""
    rng = random.Random(seed)
    t0 = t0 or datetime.now(timezone.utc)
    total = largo_ruta(ruta)
    recorrido, i, posiciones = 0.0, 0, []
    desvio_ini, desvio_fin = 0.45 * total, 0.80 * total
    while recorrido < total:
        vel = min(max(5.0, rng.gauss(vel_media_kmh, 8.0)), TOPE_KMH)
        en_desvio = anomalia and desvio_ini <= recorrido <= desvio_fin
        if en_desvio:
            vel = 0.0 if recorrido > (desvio_ini + desvio_fin) / 2 else max(5.0, rng.gauss(18, 4))
        lat, lon = punto_en_ruta(ruta, recorrido)
        if en_desvio:
            lat += 0.0065   # ~720 m hacia el norte
        lat += rng.gauss(0, 0.00003)    # ruido GPS ~ 3 m
        lon += rng.gauss(0, 0.00003)
        posiciones.append({"ts": (t0 + timedelta(seconds=i * intervalo_s)).isoformat(),
                           "lat": round(lat, 6), "lon": round(lon, 6), "velocidadKmh": round(vel, 1)})
        # Durante la parada anomala el vehiculo casi no avanza (8 m por muestra) para que el viaje termine igual.
        recorrido += vel / 3.6 * intervalo_s if vel > 0 else 8.0
        i += 1
    return posiciones


def asegurar_viaje(admin, vehiculo_id, ruta_id):
    """Deja un viaje EN_CURSO del vehiculo sobre la ruta pedida: reutiliza el que ya corre en esa ruta, finaliza
    cualquier otro que estuviera en curso, e inicia el planificado de esa ruta (o crea uno). Devuelve el id del viaje.
    `admin` es un cliente httpx con el token de un administrador."""
    viajes = admin.get("/api/viajes").raise_for_status().json()
    propios = [v for v in viajes if v["vehiculoId"] == vehiculo_id]
    for v in propios:
        if v["estado"] == "EN_CURSO":
            if v["rutaId"] == ruta_id:
                return v["id"]
            admin.post(f"/api/viajes/{v['id']}/finalizar").raise_for_status()
    planificado = next((v for v in propios if v["estado"] == "PLANIFICADO" and v["rutaId"] == ruta_id), None)
    if planificado is None:
        planificado = admin.post("/api/viajes", json={"vehiculoId": vehiculo_id, "rutaId": ruta_id}).raise_for_status().json()
    admin.post(f"/api/viajes/{planificado['id']}/iniciar").raise_for_status()
    return planificado["id"]


def faltantes(admin, flota=FLOTA):
    """Comprueba que cada patente y cada ruta de la flota existan en la base; devuelve (ids_vehiculo, ids_ruta)."""
    vehiculos = {v["patente"]: v["id"] for v in admin.get("/api/vehiculos").raise_for_status().json()}
    rutas = {r["nombre"]: r["id"] for r in admin.get("/api/rutas").raise_for_status().json()}
    for f in flota:
        for nombre in f["rutas"]:
            if f["patente"] not in vehiculos or nombre not in rutas:
                raise SystemExit(f"Falta {f['patente']} o la ruta '{nombre}' en la base. Reinicie con: docker compose down -v")
    return vehiculos, rutas


def preparar_flota(admin, flota=FLOTA):
    """Deja un viaje en curso para cada vehiculo, sobre la primera ruta de su circuito."""
    vehiculos, rutas = faltantes(admin, flota)
    for f in flota:
        asegurar_viaje(admin, vehiculos[f["patente"]], rutas[f["rutas"][0]])


def login_admin(api, email, password, intentos=120):
    """Inicia sesion como administrador; espera a que el backend este listo (arranca despues de la base)."""
    for _ in range(intentos):
        try:
            r = httpx.post(f"{api}/api/auth/login", json={"email": email, "password": password}, timeout=10)
            if r.status_code == 401:
                raise SystemExit("Credenciales de administrador invalidas para el simulador")
            r.raise_for_status()
            return httpx.Client(base_url=api, headers={"Authorization": f"Bearer {r.json()['token']}"}, timeout=10)
        except httpx.HTTPError:
            print("Esperando al backend...", flush=True)
            time.sleep(3)
    raise SystemExit("El backend no respondio a tiempo")


def enviar_viaje(api, key, viaje, intervalo, etiqueta):
    with httpx.Client(base_url=api, headers={"X-Device-Key": key}, timeout=10) as http:
        for p in viaje:
            p = {**p, "ts": datetime.now(timezone.utc).isoformat()}   # tiempo real para el mapa en vivo
            try:
                http.post("/api/posiciones", json=p).raise_for_status()
            except httpx.HTTPError as e:
                print(f"[{etiqueta}] Error de envio:", e, flush=True)
            time.sleep(intervalo)


def circuito(f, anomalo):
    """Rutas que recorre el vehiculo. Si debe desviarse (demo), solo la mas corta, para que el desvio se vea pronto."""
    if anomalo:
        return [min(f["rutas"], key=lambda n: largo_ruta(RUTAS[n]))]
    return f["rutas"]


def correr_flota(args):
    admin = login_admin(args.api, args.admin, args.password)
    vehiculos, rutas = faltantes(admin)
    for f in FLOTA:   # primer viaje de cada vehiculo (en modo demo-anomalia, sobre su ruta corta)
        asegurar_viaje(admin, vehiculos[f["patente"]], rutas[circuito(f, args.anomalia_patente == f["patente"])[0]])
    print(f"Flota lista: {len(FLOTA)} vehiculos con viaje en curso", flush=True)

    def vehiculo(f):
        anomalo = args.anomalia_patente == f["patente"]
        lista, i = circuito(f, anomalo), 0
        while True:
            nombre = lista[i % len(lista)]
            if i > 0:   # cambio de trayecto: sesion nueva (el token dura 60 min) y viaje nuevo sobre la ruta siguiente
                cli = login_admin(args.api, args.admin, args.password)
                asegurar_viaje(cli, vehiculos[f["patente"]], rutas[nombre])
                cli.close()
            viaje = generar_viaje(RUTAS[nombre], vel_media_kmh=VEL_RUTA[nombre], anomalia=anomalo)
            print(f"[{f['patente']}] {nombre}: {len(viaje)} posiciones (anomalia={anomalo})", flush=True)
            enviar_viaje(args.api, f["key"], viaje, args.intervalo, f["patente"])
            if not args.repetir:
                return
            i += 1

    hilos = [threading.Thread(target=vehiculo, args=(f,), daemon=True) for f in FLOTA]
    for h in hilos:
        h.start()
    for h in hilos:
        h.join()


def main():
    ap = argparse.ArgumentParser(description="Simulador GPS TrackPredict")
    ap.add_argument("--api", default="http://localhost:8080")
    ap.add_argument("--key", default="demo-device-key-001", help="API key del dispositivo (modo de un solo vehiculo)")
    ap.add_argument("--intervalo", type=float, default=2.0, help="segundos entre envios (tiempo real)")
    ap.add_argument("--anomalia", action="store_true", help="inyecta desvio y parada anomala (un solo vehiculo)")
    ap.add_argument("--repetir", action="store_true", help="repite el viaje indefinidamente")
    ap.add_argument("--flota", action="store_true", help="mueve los 3 vehiculos de Seguridad Demo e inicia sus viajes")
    ap.add_argument("--anomalia-patente", default=None, help="en modo flota, patente del vehiculo que se desvia (ej. AD789GH)")
    ap.add_argument("--admin", default="admin@trackpredict.com", help="usuario administrador que inicia los viajes (modo flota)")
    ap.add_argument("--password", default="Admin123!", help="clave del administrador de DEMO (modo flota)")
    args = ap.parse_args()

    if args.flota:
        correr_flota(args)
        return

    with httpx.Client(base_url=args.api, headers={"X-Device-Key": args.key}, timeout=10) as http:
        while True:
            viaje = generar_viaje(anomalia=args.anomalia)
            print(f"Nuevo viaje: {len(viaje)} posiciones (anomalia={args.anomalia})", flush=True)
            for p in viaje:
                p = {**p, "ts": datetime.now(timezone.utc).isoformat()}   # tiempo real para el mapa en vivo
                try:
                    r = http.post("/api/posiciones", json=p)
                    r.raise_for_status()
                except httpx.HTTPError as e:
                    print("Error de envio:", e, flush=True)
                time.sleep(args.intervalo)
            if not args.repetir:
                break


if __name__ == "__main__":
    main()
