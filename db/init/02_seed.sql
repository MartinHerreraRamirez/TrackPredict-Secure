-- Datos semilla SOLO PARA DESARROLLO / DEMO (todo es ficticio)
INSERT INTO empresa (nombre) VALUES ('Seguridad Demo S.A.'), ('Custodia Norte S.R.L.');

INSERT INTO usuario (empresa_id, email, password_hash, nombre, rol) VALUES
 (1,'admin@trackpredict.com',    '$2a$10$/Zwl.zaNvzoo65mYiwQEmuHtzOoLXdIF0Y16bcT4l5AMJ6zQBMr72','Administrador','ADMIN'),
 (1,'operador@trackpredict.com', '$2a$10$0CO6mN1hEoaUXQY0IXUaGunW1XYmmr2ByZii5GxzpNGDnqsKhWZSm','Operador','OPERADOR'),
 (1,'auditor@trackpredict.com',  '$2a$10$gcJEEnuVsdxhxyr7c4mphOHOPTExmSCdaejT.7NipV58L3E8ekabS','Auditor','AUDITOR'),
 (2,'admin@norte.com',   '$2a$10$iMJfxjDg/TVqGVg0Kw0WduqZkkHGIfPk2IWtE87SbV1xZWjAzigXu','Admin Norte','ADMIN');

-- Seguridad Demo opera una flota de 3 vehiculos (blindado, auto y escolta). Custodia Norte tiene 1 vehiculo,
-- pensado para demostrar el aislamiento entre empresas: su administrador no ve nada de Seguridad Demo.
INSERT INTO vehiculo (empresa_id, patente, tipo, marca, modelo) VALUES
 (1,'AB123CD','BLINDADO','Mercedes-Benz','Atego'),
 (1,'AC456EF','AUTO','Toyota','Corolla'),
 (1,'AD789GH','ESCOLTA','Ford','Ranger'),
 (2,'AE321KL','AUTO','Chevrolet','Cruze');

-- Un dispositivo GPS por vehiculo de Seguridad Demo (la clave se guarda como hash SHA-256):
--   demo-device-key-001 (AB123CD), demo-device-key-002 (AC456EF), demo-device-key-003 (AD789GH)
INSERT INTO dispositivo (empresa_id, vehiculo_id, api_key_hash) VALUES
 (1,1,'ef26d8d1af2b1842197b494153cbec61d6ba98f67a26fa849c5ee406f55c9e98'),
 (1,2,'900b0c3c91de8ef8463123f069d61a785d0936019b4da3b772e849db090c4112'),
 (1,3,'cf823bb9d7113b86443cbfc369988014b1ba7fcf85652a9f3007a8d231fe70b2');

-- Siete rutas de Buenos Aires (las mismas que recorre el simulador, en simulator/simulator.py): tres urbanas cortas
-- (ids 1 a 3) y cuatro trayectos largos de autopista (ids 4 a 7, de 19 a 51 km, a velocidad media de autopista bajo el limite de 80 km/h)
INSERT INTO ruta (empresa_id, nombre, trazado, duracion_plan_min, velocidad_max_kmh) VALUES
 (1,'Microcentro - Puerto Madero',
  ST_GeogFromText('LINESTRING(-58.3816 -34.6037, -58.3700 -34.6080, -58.3640 -34.6130, -58.3620 -34.6200)'),
  25, 60),
 (1,'Retiro - Palermo',
  ST_GeogFromText('LINESTRING(-58.3737 -34.5918, -58.3830 -34.5885, -58.3925 -34.5850, -58.4010 -34.5820)'),
  20, 60),
 (1,'Constitucion - La Boca',
  ST_GeogFromText('LINESTRING(-58.3815 -34.6277, -58.3770 -34.6320, -58.3705 -34.6365, -58.3640 -34.6395)'),
  18, 50),
 (1,'Microcentro - Aeropuerto Ezeiza',
  ST_GeogFromText('LINESTRING(-58.3816 -34.6037, -58.4 -34.615, -58.45 -34.64, -58.5 -34.66, -58.52 -34.7, -58.53 -34.75, -58.525 -34.8, -58.5358 -34.8222)'),
  56, 80),
 (1,'Retiro - Tigre',
  ST_GeogFromText('LINESTRING(-58.3737 -34.5918, -58.41 -34.57, -58.445 -34.548, -58.475 -34.52, -58.51 -34.49, -58.54 -34.46, -58.579 -34.426)'),
  56, 80),
 (1,'Puerto Madero - La Plata',
  ST_GeogFromText('LINESTRING(-58.362 -34.62, -58.36 -34.635, -58.33 -34.67, -58.27 -34.71, -58.21 -34.76, -58.15 -34.81, -58.06 -34.86, -57.99 -34.9, -57.9536 -34.9205)'),
  52, 80),
 (1,'Microcentro - San Isidro',
  ST_GeogFromText('LINESTRING(-58.3816 -34.6037, -58.405 -34.585, -58.435 -34.56, -58.465 -34.53, -58.49 -34.5, -58.511 -34.473)'),
  26, 80);

-- Corredor autorizado: 250 m a cada lado de cada ruta
INSERT INTO geocerca (empresa_id, ruta_id, nombre, area)
SELECT empresa_id, id, 'Corredor autorizado', ST_Buffer(trazado, 250)::geography
FROM ruta;

INSERT INTO conductor (empresa_id, nombre, documento) VALUES
 (1,'Conductor Demo Uno','DEMO-0001'),
 (1,'Conductor Demo Dos','DEMO-0002'),
 (1,'Conductor Demo Tres','DEMO-0003');

-- Historial de demostracion: 24 viajes FINALIZADOS de las ultimas semanas (dias habiles, ficticios). Sirven para que el
-- boton "Reentrenar modelo de ETA" quede habilitado desde el primer momento (necesita 20). Duraciones plausibles:
-- tiempo libre de la ruta, mas demora en horas pico (8-10 y 17-19) y un 10 % extra por las paradas de seguridad.
INSERT INTO viaje (empresa_id, vehiculo_id, conductor_id, ruta_id, estado, inicio_plan, fin_plan, inicio_real, fin_real) VALUES
 (1,1,1,1,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 0) + time '07:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 0) + time '07:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '7.1 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 0) + time '07:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 0) + time '07:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '7.5 minutes'),
 (1,2,2,2,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 2) + time '13:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 2) + time '13:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '5.4 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 2) + time '13:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 2) + time '13:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '5.7 minutes'),
 (1,3,3,3,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 4) + time '19:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 4) + time '19:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '3.7 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 4) + time '19:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 4) + time '19:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '3.9 minutes'),
 (1,1,1,4,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 1) + time '10:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 1) + time '10:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '39.3 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 1) + time '10:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 1) + time '10:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '41.4 minutes'),
 (1,2,2,5,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 3) + time '17:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 3) + time '17:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '46.2 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 3) + time '17:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 3) + time '17:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '48.6 minutes'),
 (1,3,3,6,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 0) + time '08:15') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 0) + time '08:15') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '68.3 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 0) + time '08:15') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 0) + time '08:15') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '71.9 minutes'),
 (1,1,1,7,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 2) + time '14:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 2) + time '14:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '23.1 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 2) + time '14:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 2) + time '14:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '24.3 minutes'),
 (1,2,2,1,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 4) + time '21:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 4) + time '21:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '4.4 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 4) + time '21:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 7 + 4) + time '21:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '4.6 minutes'),
 (1,3,3,2,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 1) + time '11:45') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 1) + time '11:45') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '4.9 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 1) + time '11:45') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 1) + time '11:45') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '5.2 minutes'),
 (1,1,1,3,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 3) + time '18:15') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 3) + time '18:15') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '4.9 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 3) + time '18:15') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 3) + time '18:15') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '5.2 minutes'),
 (1,2,2,4,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 0) + time '09:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 0) + time '09:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '46.9 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 0) + time '09:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 0) + time '09:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '49.4 minutes'),
 (1,3,3,5,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 2) + time '16:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 2) + time '16:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '32.6 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 2) + time '16:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 2) + time '16:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '34.3 minutes'),
 (1,1,1,6,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 4) + time '07:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 4) + time '07:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '59.5 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 4) + time '07:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 4) + time '07:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '62.6 minutes'),
 (1,2,2,7,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 1) + time '13:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 1) + time '13:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '22.5 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 1) + time '13:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 1) + time '13:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '23.7 minutes'),
 (1,3,3,1,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 3) + time '19:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 3) + time '19:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '6.7 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 3) + time '19:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 3) + time '19:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '7.1 minutes'),
 (1,1,1,2,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 0) + time '10:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 0) + time '10:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '6.9 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 0) + time '10:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 14 + 0) + time '10:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '7.3 minutes'),
 (1,2,2,3,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 2) + time '17:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 2) + time '17:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '7.7 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 2) + time '17:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 2) + time '17:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '8.1 minutes'),
 (1,3,3,4,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 4) + time '08:15') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 4) + time '08:15') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '46.9 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 4) + time '08:15') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 4) + time '08:15') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '49.4 minutes'),
 (1,1,1,5,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 1) + time '14:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 1) + time '14:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '29.0 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 1) + time '14:30') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 1) + time '14:30') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '30.5 minutes'),
 (1,2,2,6,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 3) + time '21:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 3) + time '21:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '51.5 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 3) + time '21:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 3) + time '21:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '54.2 minutes'),
 (1,3,3,7,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 0) + time '11:45') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 0) + time '11:45') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '24.5 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 0) + time '11:45') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 0) + time '11:45') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '25.8 minutes'),
 (1,1,1,1,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 2) + time '18:15') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 2) + time '18:15') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '5.8 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 2) + time '18:15') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 2) + time '18:15') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '6.1 minutes'),
 (1,2,2,2,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 4) + time '09:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 4) + time '09:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '6.7 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 4) + time '09:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 4) + time '09:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '7.1 minutes'),
 (1,3,3,3,'FINALIZADO', (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 1) + time '16:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'),
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 1) + time '16:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '5.7 minutes',
  (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 1) + time '16:00') AT TIME ZONE 'America/Argentina/Buenos_Aires'), (((date_trunc('week', now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::date - 21 + 1) + time '16:00') AT TIME ZONE 'America/Argentina/Buenos_Aires') + interval '6.0 minutes');

-- Un viaje planificado por vehiculo, sobre el trayecto largo con el que empieza su circuito; el simulador en
-- modo flota los inicia solo y despues cada vehiculo pasa a las otras rutas de su circuito
INSERT INTO viaje (empresa_id, vehiculo_id, conductor_id, ruta_id, estado, inicio_plan, fin_plan) VALUES
 (1,1,1,4,'PLANIFICADO', now(), now() + interval '56 minutes'),
 (1,2,2,5,'PLANIFICADO', now(), now() + interval '56 minutes'),
 (1,3,3,6,'PLANIFICADO', now(), now() + interval '52 minutes');
