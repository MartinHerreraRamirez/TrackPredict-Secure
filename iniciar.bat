@echo off
REM TrackPredict Secure - levanta todo (base, servicios, web y simulador de la flota) y abre el navegador.
REM Requiere Docker Desktop abierto. La primera vez tarda varios minutos.
cd /d "%~dp0"
docker compose up --build -d
if errorlevel 1 (
  echo.
  echo No se pudo levantar. Verifique que Docker Desktop este abierto y copie el error de arriba.
  pause
  exit /b 1
)
echo.
echo Esperando a que el backend este listo...
:esperar
timeout /t 3 /nobreak >nul
curl -s -o nul -f http://localhost:8080/actuator/health || goto esperar
echo Listo. Abriendo http://localhost:8081 (admin@trackpredict.com / Admin123!)
start http://localhost:8081
echo El simulador inicia solo los 3 viajes y mueve la flota en unos segundos.
pause
