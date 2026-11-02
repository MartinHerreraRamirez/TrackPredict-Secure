@echo off
REM Pausa el simulador normal y hace que la escolta AD789GH se desvie y se detenga (una vuelta).
REM Despues vuelve a poner en marcha la flota normal. Con la app ya levantada (iniciar.bat).
cd /d "%~dp0"
docker compose stop simulator
REM Reconstruye la imagen del simulador por si se modifico simulator.py (es rapido si no hubo cambios)
docker compose build simulator
docker compose run --rm simulator --api http://backend:8080 --flota --anomalia-patente AD789GH
docker compose up -d simulator
echo.
echo Para ver la alerta de trayecto anomalo: en el panel, "Ver prediccion" en el viaje de AD789GH.
pause
