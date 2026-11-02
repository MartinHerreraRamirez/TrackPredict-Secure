@echo off
REM Detiene todo y conserva los datos. Para empezar de cero use reiniciar-limpio.bat
cd /d "%~dp0"
docker compose down
pause
