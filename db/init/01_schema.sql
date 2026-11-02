-- TrackPredict Secure - esquema inicial (PostgreSQL + PostGIS)
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE empresa (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    activa      BOOLEAN NOT NULL DEFAULT TRUE,
    creada_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Roles de empresa (RBAC): ADMIN, OPERADOR, SUPERVISOR, AUDITOR
CREATE TABLE usuario (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id),
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nombre        VARCHAR(120) NOT NULL,
    rol           VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN','OPERADOR','SUPERVISOR','AUDITOR')),
    activo        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE vehiculo (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
    patente     VARCHAR(12) NOT NULL,
    tipo        VARCHAR(20) NOT NULL CHECK (tipo IN ('BLINDADO','AUTO','ESCOLTA')),
    marca       VARCHAR(60),
    modelo      VARCHAR(60),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, patente)
);

CREATE TABLE conductor (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
    nombre      VARCHAR(120) NOT NULL,
    documento   VARCHAR(20) NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, documento)
);

-- Dispositivo GPS: se autentica con una API key propia (se guarda solo su hash SHA-256)
CREATE TABLE dispositivo (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id),
    vehiculo_id   BIGINT NOT NULL REFERENCES vehiculo(id),
    api_key_hash  CHAR(64) NOT NULL UNIQUE,
    activo        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE ruta (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
    nombre      VARCHAR(120) NOT NULL,
    trazado     geography(LineString, 4326) NOT NULL,
    duracion_plan_min INT NOT NULL,
    velocidad_max_kmh INT NOT NULL DEFAULT 80
);

CREATE TABLE geocerca (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
    ruta_id     BIGINT NOT NULL REFERENCES ruta(id),   -- corredor autorizado de esa ruta
    nombre      VARCHAR(120) NOT NULL,
    area        geography(Polygon, 4326) NOT NULL
);
CREATE INDEX idx_geocerca_ruta ON geocerca (ruta_id);

CREATE TABLE viaje (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id),
    vehiculo_id   BIGINT NOT NULL REFERENCES vehiculo(id),
    conductor_id  BIGINT REFERENCES conductor(id),
    ruta_id       BIGINT REFERENCES ruta(id),
    estado        VARCHAR(15) NOT NULL DEFAULT 'PLANIFICADO'
                  CHECK (estado IN ('PLANIFICADO','EN_CURSO','FINALIZADO','CANCELADO')),
    inicio_plan   TIMESTAMPTZ,
    fin_plan      TIMESTAMPTZ,
    inicio_real   TIMESTAMPTZ,
    fin_real      TIMESTAMPTZ
);

CREATE TABLE posicion (
    id           BIGSERIAL PRIMARY KEY,
    empresa_id   BIGINT NOT NULL REFERENCES empresa(id),
    vehiculo_id  BIGINT NOT NULL REFERENCES vehiculo(id),
    viaje_id     BIGINT REFERENCES viaje(id),
    ts           TIMESTAMPTZ NOT NULL,
    lat          DOUBLE PRECISION NOT NULL,
    lon          DOUBLE PRECISION NOT NULL,
    velocidad_kmh DOUBLE PRECISION NOT NULL DEFAULT 0
);
CREATE INDEX idx_posicion_vehiculo_ts ON posicion (vehiculo_id, ts DESC);
CREATE INDEX idx_posicion_empresa_ts ON posicion (empresa_id, ts DESC);

CREATE TABLE alerta (
    id           BIGSERIAL PRIMARY KEY,
    empresa_id   BIGINT NOT NULL REFERENCES empresa(id),
    vehiculo_id  BIGINT NOT NULL REFERENCES vehiculo(id),
    tipo         VARCHAR(30) NOT NULL
                 CHECK (tipo IN ('EXCESO_VELOCIDAD','SALIDA_GEOCERCA','PARADA_PROLONGADA','ANOMALIA_TRAYECTO')),
    detalle      VARCHAR(255),
    estado       VARCHAR(15) NOT NULL DEFAULT 'NUEVA'
                 CHECK (estado IN ('NUEVA','RECONOCIDA','EN_ATENCION','CERRADA')),
    creada_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Bitacora de auditoria: solo INSERT desde la aplicacion
CREATE TABLE auditoria (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT,
    usuario_id  BIGINT,
    accion      VARCHAR(60) NOT NULL,
    recurso     VARCHAR(120),
    ts          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE OR REPLACE FUNCTION auditoria_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La bitacora de auditoria es de solo insercion';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_auditoria_no_update BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_inmutable();
