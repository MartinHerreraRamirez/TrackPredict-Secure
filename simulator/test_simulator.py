from simulator import RUTA, generar_viaje


def dist_min_a_ruta(p):
    from simulator import haversine_m
    pts = [(p["lat"], p["lon"])]
    # aproximacion: distancia al vertice mas cercano de una version densificada de la ruta
    from simulator import punto_en_ruta, largo_ruta
    dens = [punto_en_ruta(RUTA, d) for d in range(0, int(largo_ruta(RUTA)), 20)]
    return min(haversine_m(pts[0], q) for q in dens)


def test_viaje_normal_sigue_la_ruta():
    v = generar_viaje(seed=1)
    assert len(v) > 50
    assert max(dist_min_a_ruta(p) for p in v) < 60


def test_viaje_anomalo_se_desvia_y_se_detiene():
    v = generar_viaje(anomalia=True, seed=1)
    assert max(dist_min_a_ruta(p) for p in v) > 400
    assert any(p["velocidadKmh"] == 0.0 for p in v)


def test_es_deterministico_con_seed():
    from datetime import datetime, timezone
    t0 = datetime(2026, 10, 12, tzinfo=timezone.utc)
    assert generar_viaje(seed=7, t0=t0) == generar_viaje(seed=7, t0=t0)


# ---- Modo flota ----
import httpx
import simulator as sim


def test_cada_ruta_de_la_flota_existe_y_tiene_largo_razonable():
    for f in sim.FLOTA:
        assert len(f["rutas"]) >= 2                      # circuito: un trayecto largo y al menos uno corto
        for nombre in f["rutas"]:
            assert nombre in sim.RUTAS and nombre in sim.VEL_RUTA
            assert 1000 < sim.largo_ruta(sim.RUTAS[nombre]) < 60000
    assert len({f["key"] for f in sim.FLOTA}) == 3      # un dispositivo distinto por vehiculo
    # hay trayectos largos de verdad (mas de 25 km) y todas las rutas de la semilla estan en el simulador
    assert max(sim.largo_ruta(r) for r in sim.RUTAS.values()) > 45000
    assert len(sim.RUTAS) == 7 and set(sim.RUTAS) == set(sim.VEL_RUTA)


def test_circuito_en_modo_anomalia_usa_solo_la_ruta_mas_corta():
    f = sim.FLOTA[2]                                     # AD789GH: La Plata (larga) y La Boca (corta)
    assert sim.circuito(f, False) == f["rutas"]
    assert sim.circuito(f, True) == ["Constitucion - La Boca"]


def _admin_falso(viajes, vehiculos, rutas, finalizables=False):
    """Cliente httpx contra un backend falso en memoria. `viajes` es la lista de viajes existentes (se modifica)."""
    llamadas = []

    def handler(req: httpx.Request) -> httpx.Response:
        llamadas.append((req.method, req.url.path))
        if req.url.path == "/api/vehiculos":
            return httpx.Response(200, json=vehiculos)
        if req.url.path == "/api/rutas":
            return httpx.Response(200, json=rutas)
        if req.url.path == "/api/viajes" and req.method == "GET":
            return httpx.Response(200, json=viajes)
        if req.url.path == "/api/viajes" and req.method == "POST":
            nuevo = {"id": 100 + len(viajes), "vehiculoId": 0, "rutaId": 0, "estado": "PLANIFICADO"}
            import json
            nuevo.update(json.loads(req.content))
            viajes.append(nuevo)
            return httpx.Response(201, json=nuevo)
        if req.url.path.endswith("/iniciar"):
            vid = int(req.url.path.split("/")[3])
            next(v for v in viajes if v["id"] == vid)["estado"] = "EN_CURSO"
            return httpx.Response(200, json={})
        if finalizables and req.url.path.endswith("/finalizar"):
            vid = int(req.url.path.split("/")[3])
            next(v for v in viajes if v["id"] == vid)["estado"] = "FINALIZADO"
            return httpx.Response(200, json={})
        return httpx.Response(404)

    return httpx.Client(base_url="http://x", transport=httpx.MockTransport(handler)), llamadas


RUTAS_BD = ["Microcentro - Puerto Madero", "Retiro - Palermo", "Constitucion - La Boca", "Microcentro - Aeropuerto Ezeiza",
            "Retiro - Tigre", "Puerto Madero - La Plata", "Microcentro - San Isidro"]


def test_preparar_flota_inicia_los_planificados_y_crea_los_faltantes():
    vehiculos = [{"id": 1, "patente": "AB123CD"}, {"id": 2, "patente": "AC456EF"}, {"id": 3, "patente": "AD789GH"}]
    rutas = [{"id": i + 1, "nombre": n} for i, n in enumerate(RUTAS_BD)]
    # primeras rutas del circuito: Ezeiza (4), Tigre (5), La Plata (6)
    viajes = [{"id": 10, "vehiculoId": 1, "rutaId": 4, "estado": "PLANIFICADO"},
              {"id": 11, "vehiculoId": 2, "rutaId": 5, "estado": "EN_CURSO"}]      # el vehiculo 3 no tiene viaje
    admin, llamadas = _admin_falso(viajes, vehiculos, rutas)
    sim.preparar_flota(admin)
    assert {(v["vehiculoId"], v["estado"]) for v in viajes} == {(1, "EN_CURSO"), (2, "EN_CURSO"), (3, "EN_CURSO")}
    assert ("POST", "/api/viajes/11/iniciar") not in llamadas                          # el que ya corria no se toca
    assert sum(1 for m, p in llamadas if m == "POST" and p == "/api/viajes") == 1      # solo se crea el del vehiculo 3


def test_asegurar_viaje_finaliza_el_que_corre_en_otra_ruta():
    vehiculos = [{"id": 1, "patente": "AB123CD"}]
    rutas = [{"id": i + 1, "nombre": n} for i, n in enumerate(RUTAS_BD)]
    viajes = [{"id": 10, "vehiculoId": 1, "rutaId": 4, "estado": "EN_CURSO"}]
    admin, llamadas = _admin_falso(viajes, vehiculos, rutas, finalizables=True)
    nuevo = sim.asegurar_viaje(admin, 1, 1)                                            # pasa a Microcentro - Puerto Madero
    assert ("POST", "/api/viajes/10/finalizar") in llamadas
    assert viajes[0]["estado"] == "FINALIZADO"
    assert next(v for v in viajes if v["id"] == nuevo)["estado"] == "EN_CURSO"
